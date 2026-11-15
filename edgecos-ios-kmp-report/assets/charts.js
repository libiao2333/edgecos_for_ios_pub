(function () {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();

  // --- Chart: iOS 端代码构成 ---
  var codeEl = document.getElementById('chart-code');
  if (codeEl) {
    var codeChart = echarts.init(codeEl, null, { renderer: 'svg' });
    codeChart.setOption({
      animation: false,
      grid: { left: 150, right: 60, top: 24, bottom: 32 },
      tooltip: {
        appendToBody: true,
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        valueFormatter: function (v) { return v.toLocaleString() + ' 行'; }
      },
      xAxis: {
        type: 'value',
        axisLabel: { color: muted, fontSize: 11 },
        splitLine: { lineStyle: { color: rule } },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'category',
        data: ['iOS 原生胶水', '共享领域 :domain', '共享逻辑 :shared', '共享 UI :composeApp'],
        axisLabel: { color: ink, fontSize: 12 },
        axisLine: { lineStyle: { color: rule } },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        barWidth: 22,
        data: [
          { value: 187, itemStyle: { color: accent2 } },
          { value: 414, itemStyle: { color: accent, opacity: 0.55 } },
          { value: 2166, itemStyle: { color: accent, opacity: 0.78 } },
          { value: 3020, itemStyle: { color: accent } }
        ],
        label: {
          show: true,
          position: 'right',
          color: ink,
          fontSize: 12,
          fontFamily: 'Geist Mono, monospace',
          formatter: function (p) { return p.value.toLocaleString(); }
        },
        itemStyle: { borderRadius: [0, 6, 6, 0] }
      }]
    });
    window.addEventListener('resize', function () { codeChart.resize(); });
  }

  // --- Chart: 六维完成度 ---
  var radarEl = document.getElementById('chart-radar');
  if (radarEl) {
    var radarChart = echarts.init(radarEl, null, { renderer: 'svg' });
    radarChart.setOption({
      animation: false,
      tooltip: { appendToBody: true, trigger: 'item' },
      radar: {
        center: ['50%', '54%'],
        radius: '64%',
        indicator: [
          { name: '源码复用', max: 100 },
          { name: '页面覆盖', max: 100 },
          { name: '核心功能可用', max: 100 },
          { name: '总体功能完善性', max: 100 },
          { name: '自动化验证', max: 100 },
          { name: '视觉一致', max: 100 }
        ],
        axisName: { color: ink, fontSize: 12 },
        splitLine: { lineStyle: { color: rule } },
        splitArea: { show: false },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'radar',
        symbolSize: 5,
        data: [{
          value: [97, 94, 95, 75, 80, 85],
          name: 'Ver 1.0.0',
          lineStyle: { color: accent, width: 2 },
          itemStyle: { color: accent },
          areaStyle: { color: accent, opacity: 0.16 }
        }]
      }]
    });
    window.addEventListener('resize', function () { radarChart.resize(); });
  }
})();
