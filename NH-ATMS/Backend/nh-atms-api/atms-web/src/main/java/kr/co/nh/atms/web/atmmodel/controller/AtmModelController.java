package kr.co.nh.atms.web.atmmodel.controller;

import jakarta.validation.Valid;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.common.response.ApiResponse;
import kr.co.nh.atms.web.atmmodel.dto.AtmModelSearchDto;
import kr.co.nh.atms.web.atmmodel.entity.AtmModel;
import kr.co.nh.atms.web.atmmodel.service.AtmModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm-model")
@RequiredArgsConstructor
public class AtmModelController {

    private final AtmModelService service;

    @GetMapping
    public ApiResponse<PageResult<AtmModel>> list(@ModelAttribute AtmModelSearchDto search) {
        return ApiResponse.ok(service.search(search));
    }

    @GetMapping("/{modelCd}")
    public ApiResponse<AtmModel> get(@PathVariable String modelCd) {
        return ApiResponse.ok(service.get(modelCd));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody AtmModel model) {
        service.create(model);
        return ApiResponse.ok(null, "등록 성공");
    }

    @PutMapping("/{modelCd}")
    public ApiResponse<Void> update(@PathVariable String modelCd,
                                    @Valid @RequestBody AtmModel model) {
        model.setModelCd(modelCd);
        service.update(model);
        return ApiResponse.ok(null, "수정 성공");
    }

    @DeleteMapping("/{modelCd}")
    public ApiResponse<Void> delete(@PathVariable String modelCd) {
        service.delete(modelCd);
        return ApiResponse.ok(null, "삭제 성공");
    }
}
