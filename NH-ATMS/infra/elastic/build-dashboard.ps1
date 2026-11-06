# =====================================================================
# build-dashboard.ps1 — NH-ATMS 전자저널 운영 대시보드를 Kibana 에 생성
# RFP 「전자저널 가동현황 모니터링」 요건 데모. 데이터뷰 atms-journal 전제.
# 사용: .\build-dashboard.ps1 [http://192.168.28.21:19601]
# =====================================================================
param([string]$Kibana = "http://192.168.28.21:19601")

$IDX = "atms-journal"          # 데이터뷰 id
$VER = "8.18.2"

# 시각화 정의 (agg-based visualization) — visState 를 문자열로 직렬화해 삽입
function VizObj($id, $title, $visState) {
  $ss = '{"query":{"query":"","language":"kuery"},"filter":[],"indexRefName":"kibanaSavedObjectMeta.searchSourceJSON.index"}'
  [ordered]@{
    id = $id; type = "visualization"
    attributes = [ordered]@{
      title = $title; visState = ($visState | ConvertTo-Json -Depth 30 -Compress)
      uiStateJSON = "{}"; description = ""; version = 1
      kibanaSavedObjectMeta = @{ searchSourceJSON = $ss }
    }
    references = @(@{ name = "kibanaSavedObjectMeta.searchSourceJSON.index"; type = "index-pattern"; id = $IDX })
  }
}

$vizMetric = VizObj "nhatms-metric" "총 수신 건수" ([ordered]@{
  title="총 수신 건수"; type="metric"
  params=@{ addTooltip=$true; addLegend=$false; type="metric"; metric=@{ percentageMode=$false; useRanges=$false; colorSchema="Green to Red"; metricColorMode="None"; labels=@{show=$true}; style=@{bgColor=$false; fontSize=48} } }
  aggs=@(@{ id="1"; enabled=$true; type="count"; schema="metric"; params=@{} })
})

$vizTrend = VizObj "nhatms-trend" "저널 유입 추이" ([ordered]@{
  title="저널 유입 추이"; type="histogram"
  params=@{ type="histogram"; grid=@{categoryLines=$false}
    categoryAxes=@(@{ id="CategoryAxis-1"; type="category"; position="bottom"; show=$true; scale=@{type="linear"}; labels=@{show=$true; filter=$true; truncate=100}; title=@{} })
    valueAxes=@(@{ id="ValueAxis-1"; name="LeftAxis-1"; type="value"; position="left"; show=$true; scale=@{type="linear"; mode="normal"}; labels=@{show=$true; rotate=0; filter=$false; truncate=100}; title=@{text="건수"} })
    seriesParams=@(@{ show=$true; type="histogram"; mode="stacked"; data=@{label="건수"; id="1"}; valueAxis="ValueAxis-1"; drawLinesBetweenPoints=$true; lineWidth=2; showCircles=$true })
    addTooltip=$true; addLegend=$true; legendPosition="right"; times=@(); addTimeMarker=$false }
  aggs=@(
    @{ id="1"; enabled=$true; type="count"; schema="metric"; params=@{} }
    @{ id="2"; enabled=$true; type="date_histogram"; schema="segment"; params=@{ field="receivedAt"; useNormalizedEsInterval=$true; interval="auto"; drop_partials=$false; min_doc_count=1; extended_bounds=@{} } }
  )
})

$vizKind = VizObj "nhatms-kind" "저널 유형(jnlKind) 분포" ([ordered]@{
  title="저널 유형 분포"; type="pie"
  params=@{ type="pie"; addTooltip=$true; addLegend=$true; legendPosition="right"; isDonut=$true; labels=@{show=$true; values=$true; last_level=$true; truncate=100} }
  aggs=@(
    @{ id="1"; enabled=$true; type="count"; schema="metric"; params=@{} }
    @{ id="2"; enabled=$true; type="terms"; schema="segment"; params=@{ field="jnlKind"; size=10; order="desc"; orderBy="1" } }
  )
})

$vizDevice = VizObj "nhatms-device" "기기별 수신 Top" ([ordered]@{
  title="기기별 수신 Top"; type="table"
  params=@{ perPage=10; showPartialRows=$false; showMetricsAtAllLevels=$false; showTotal=$false; totalFunc="sum"; percentageCol="" }
  aggs=@(
    @{ id="1"; enabled=$true; type="count"; schema="metric"; params=@{} }
    @{ id="2"; enabled=$true; type="terms"; schema="bucket"; params=@{ field="termGroupId"; size=10; order="desc"; orderBy="1" } }
    @{ id="3"; enabled=$true; type="terms"; schema="bucket"; params=@{ field="termId"; size=5; order="desc"; orderBy="1" } }
  )
})

# 대시보드 (패널 배치 — 그리드 폭 48)
$panels = @(
  @{ version=$VER; type="visualization"; gridData=@{x=0;  y=0; w=12; h=8;  i="1"}; panelIndex="1"; embeddableConfig=@{}; panelRefName="panel_1" }
  @{ version=$VER; type="visualization"; gridData=@{x=12; y=0; w=36; h=8;  i="2"}; panelIndex="2"; embeddableConfig=@{}; panelRefName="panel_2" }
  @{ version=$VER; type="visualization"; gridData=@{x=0;  y=8; w=24; h=13; i="3"}; panelIndex="3"; embeddableConfig=@{}; panelRefName="panel_3" }
  @{ version=$VER; type="visualization"; gridData=@{x=24; y=8; w=24; h=13; i="4"}; panelIndex="4"; embeddableConfig=@{}; panelRefName="panel_4" }
)
$dashboard = [ordered]@{
  id = "nhatms-journal-dashboard"; type = "dashboard"
  attributes = [ordered]@{
    title = "NH-ATMS 전자저널 운영 대시보드"
    description = "저널 수신 현황 — RFP 전자저널 가동현황 모니터링"
    panelsJSON = ($panels | ConvertTo-Json -Depth 20 -Compress)
    optionsJSON = '{"useMargins":true,"hidePanelTitles":false}'
    version = 1; timeRestore = $true; timeTo = "now"; timeFrom = "now-24h"
    refreshInterval = @{ pause = $false; value = 10000 }
    kibanaSavedObjectMeta = @{ searchSourceJSON = '{"query":{"query":"","language":"kuery"},"filter":[]}' }
  }
  references = @(
    @{ name="panel_1"; type="visualization"; id="nhatms-metric" }
    @{ name="panel_2"; type="visualization"; id="nhatms-trend" }
    @{ name="panel_3"; type="visualization"; id="nhatms-kind" }
    @{ name="panel_4"; type="visualization"; id="nhatms-device" }
  )
}

# NDJSON 조립 (객체당 1줄) + _import
$objs = @($vizMetric, $vizTrend, $vizKind, $vizDevice, $dashboard)
$ndjson = ($objs | ForEach-Object { $_ | ConvertTo-Json -Depth 40 -Compress }) -join "`n"
$tmp = Join-Path $env:TEMP "nhatms-dashboard.ndjson"
[System.IO.File]::WriteAllText($tmp, $ndjson, (New-Object System.Text.UTF8Encoding $false))

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$body = "--$boundary$LF" +
        "Content-Disposition: form-data; name=`"file`"; filename=`"dashboard.ndjson`"$LF" +
        "Content-Type: application/ndjson$LF$LF" +
        (Get-Content $tmp -Raw) + "$LF--$boundary--$LF"
$resp = Invoke-RestMethod -Uri "$Kibana/api/saved_objects/_import?overwrite=true" -Method Post `
  -Headers @{ "kbn-xsrf" = "true" } -ContentType "multipart/form-data; boundary=$boundary" -Body $body
"success: $($resp.success) / count: $($resp.successCount)"
if ($resp.errors) { "errors:"; $resp.errors | ConvertTo-Json -Depth 10 }
"대시보드 URL: $Kibana/app/dashboards#/view/nhatms-journal-dashboard"