package kr.co.nh.atms.journal.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.nh.atms.journal.dto.JifMessage;
import kr.co.nh.atms.journal.dto.JournalReceiveResult;
import kr.co.nh.atms.journal.service.JournalDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * agent(.NET) 바이너리 저널 수신 — AS-IS atmr ServletHandler /agent/put.jnl.data.do 포팅.
 *
 * wire format (Ag2AtmsJounalTransferWorker 주석 기준, little-endian / euc-kr):
 *   체인 마스킹 키(uint8)
 *   저널 사이즈(int32) + 저널 데이터(idx: head 60B + body)
 *   명세표 사이즈(int32) + 명세표 데이터
 *   고객/수취/카드 이미지 사이즈(int32) + 이미지  × 3
 *
 * 응답: 성공 {} / 실패 {"##emsg##": "..."} — agent 는 ##emsg## 공백이면 로컬 파일 삭제.
 * AS-IS 와 동일하게 저널 처리 오류는 T_JOURNAL_ERROR 로 흡수하고 agent 에는 성공을 응답
 * (agent 무한 재전송 방지 — 재처리는 서버측 JournalErrorRetryWorker 담당).
 */
@Slf4j
@RestController
public class LegacyJournalDataController {

    private final JournalDispatchService dispatchService;
    private final ObjectMapper objectMapper;
    private final JifSpecConverter jif;
    private final String imageDir;

    public LegacyJournalDataController(JournalDispatchService dispatchService,
                                       ObjectMapper objectMapper,
                                       JifSpecConverter jif,
                                       @Value("${journal.legacy.image-dir:./images}") String imageDir) {
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
        this.jif = jif;
        this.imageDir = imageDir;
    }

    @PostMapping(
            value = {"/atms/agent/put.jnl.data.do", "/atmr/agent/put.jnl.data.do"},
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String putJnlData(HttpServletRequest req) {
        try {
            JsonObject jnl = convert(req);

            JifMessage msg = objectMapper.readValue(jnl.toString(), JifMessage.class);
            JournalReceiveResult result = dispatchService.dispatch(msg);
            if (!result.isStored()) {
                log.warn("legacy jnl stored=false kind={} reason={} (T_JOURNAL_ERROR 보존, agent 에는 성공 응답)",
                        result.getJnlKind(), result.getReason());
            }
            return "{}";
        } catch (Exception e) {
            log.error("legacy jnl receive failed", e);
            // 전송 계층 오류(프로토콜 위반 등)만 agent 에 실패 통보 → agent 가 재시도/fail 백업
            return "{\"##emsg##\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
        }
    }

    /** AS-IS ServletHandler.convert() 포팅 — 체인 마스킹 해제 + JIF 변환 + 명세표/이미지 추출 */
    private JsonObject convert(HttpServletRequest req) throws IOException {
        JsonObject jnl;
        JsonObject ext;
        try (InputStream ins = req.getInputStream()) {
            byte[] blen = new byte[4];
            ByteBuffer bb = ByteBuffer.wrap(blen);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int io;

            //체인 마스킹 키 (개별 데이터 단위 마스킹)
            byte masking = (byte) ins.read();

            //저널 사이즈(int32)
            recvn(ins, blen, 0, 4);
            io = bb.getInt(0);
            if (io <= 0) {
                throw new IOException("jnl transfer protocol exception");
            }
            //저널
            {
                byte[] bin = new byte[io];
                recvn(ins, bin, 0, bin.length);
                jnl = jif.toJson(unmask(bin, masking));
                ext = new JsonObject();
                jnl.add("extra", ext);
            }

            //명세표 사이즈(int32)
            recvn(ins, blen, 0, 4);
            io = bb.getInt(0);
            if (io > 0) {
                byte[] bin = new byte[io];
                recvn(ins, bin, 0, bin.length);
                ext.addProperty("명세표", new String(unmask(bin, masking), jif.charset()));
            } else {
                ext.addProperty("명세표", "");
            }

            //1:고객, 2:수취, 3:카드 이미지
            Path imgdir;
            String imgkey;
            {
                JsonObject jhead = jnl.getAsJsonObject("head");
                String yyyymmdd = s(jhead, "yyyy") + s(jhead, "mm") + s(jhead, "dd");
                String hhmissff = s(jhead, "hh") + s(jhead, "mi") + s(jhead, "ss") + s(jhead, "fff") + s(jhead, "flag");
                String atmId = s(jhead, "atm_id");
                // ~/img/yyyymmdd/영업점코드(5).기기번호(3).저널시간(10).이미지형식(2).마스킹(2).암호화여부(1).jpg
                imgkey = String.format("%s.%s.%s", atmId.substring(0, 5), atmId.substring(5, 8), hhmissff);
                imgdir = Paths.get(imageDir, "img", yyyymmdd);
                Files.createDirectories(imgdir);
            }
            String[] iif = new String[]{"", "고객IMG", "수취IMG", "카드IMG"};
            for (int i = 1; i <= 3; i++) {
                recvn(ins, blen, 0, 4);
                io = bb.getInt(0);
                if (io <= 0) {
                    ext.addProperty(iif[i], "");
                    continue;
                }
                byte[] bin = new byte[io];
                recvn(ins, bin, 0, bin.length);
                bin = unmask(bin, masking);
                // AS-IS 는 KSign(img_encrypt)으로 재암호화 후 저장 — NH 이미지 암호화 표준 확정 전이므로
                // 평문 저장(마스킹 00, 암호화 0). 파일명 규약은 AS-IS 유지.
                Path path = imgdir.resolve(String.format("%s.%02d.%02x.%d.jpg", imgkey, i, 0x00, 0));
                Files.write(path, bin);
                ext.addProperty(Integer.toString(i), path.toString());
            }
        }
        return jnl;
    }

    /** agent Mask()의 역함수 — 체인 XOR (각 바이트를 직전 원문 바이트로 XOR, 시드는 키) */
    static byte[] unmask(byte[] data, byte key) {
        byte mask = key;
        for (int i = 0; i < data.length; i++) {
            data[i] ^= mask;
            mask = data[i];
        }
        return data;
    }

    static int recvn(InputStream ins, byte[] buf, int off, int len) throws IOException {
        if (len <= 0) { return 0; }
        int io, tot = 0;
        while (true) {
            io = ins.read(buf, off, len);
            if (io <= 0) { return io; }
            off += io;
            len -= io;
            tot += io;
            if (len == 0) break;
        }
        return tot;
    }

    private static String s(JsonObject o, String key) {
        return o != null && o.has(key) ? o.get(key).getAsString().trim() : "";
    }

    /** dispatch 로 넘기기 전 JifMessage 뼈대 확인용 (테스트에서 사용) */
    static Map<String, Object> toMap(JsonObject o) {
        Map<String, Object> m = new LinkedHashMap<>();
        o.entrySet().forEach(e -> m.put(e.getKey(), e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : e.getValue().toString()));
        return m;
    }
}
