<template>
  <el-dialog
    v-model="dialogVisible"
    width="980px"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    align-center
    class="import-dialog-wrap"
    @closed="handleClosed"
  >
    <template #header>
      <div class="modal-header-bar">
        <div class="modal-title-text">
          <span>批量账单导入向导</span>
        </div>
      </div>
    </template>

    <!-- 可点击回退的四步步骤条 -->
    <div class="wizard-stepper">
      <div
        class="stepper-item"
        :class="{ 'is-active': currentStep === 0, 'is-done': currentStep > 0, 'is-clickable': currentStep > 0 && !isFinishStep }"
        @click="currentStep > 0 && !isFinishStep && goToStep(0)"
      >
        <div class="stepper-index num-font">
          <el-icon v-if="currentStep > 0"><Check /></el-icon>
          <span v-else>1</span>
        </div>
        <div class="stepper-text">
          <span class="stepper-title">选择渠道与文件</span>
        </div>
      </div>

      <div
        class="stepper-item"
        :class="{ 'is-active': currentStep === 1, 'is-done': currentStep > 1, 'is-clickable': currentStep > 1 && !isFinishStep }"
        @click="currentStep > 1 && !isFinishStep && goToStep(1)"
      >
        <div class="stepper-index num-font">
          <el-icon v-if="currentStep > 1"><Check /></el-icon>
          <span v-else>2</span>
        </div>
        <div class="stepper-text">
          <span class="stepper-title">统一分类确认</span>
        </div>
      </div>

      <div
        class="stepper-item"
        :class="{ 'is-active': currentStep === 2, 'is-done': currentStep > 2, 'is-clickable': currentStep > 2 && !isFinishStep }"
        @click="currentStep > 2 && !isFinishStep && goToStep(2)"
      >
        <div class="stepper-index num-font">
          <el-icon v-if="currentStep > 2"><Check /></el-icon>
          <span v-else>3</span>
        </div>
        <div class="stepper-text">
          <span class="stepper-title">预检与排重</span>
        </div>
      </div>

      <div class="stepper-item" :class="{ 'is-active': currentStep === 3 }">
        <div class="stepper-index num-font">
          <el-icon v-if="currentStep === 3"><Check /></el-icon>
          <span v-else>4</span>
        </div>
        <div class="stepper-text">
          <span class="stepper-title">完成入库</span>
        </div>
      </div>
    </div>

    <!-- ================= 步骤 1：选择渠道与上传文件 ================= -->
    <div v-if="currentStep === 0" class="step-main-body">
      <div class="section-header-row">
        <div class="section-label">选择账单渠道</div>
      </div>

      <div class="channel-grid">
        <div class="channel-card channel-wechat" :class="{ 'is-selected': currentChannel === 'wechat' }" @click="selectChannel('wechat')">
          <div class="channel-check-badge">
            <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="2.5 6 5 8.5 9.5 3.5"/></svg>
          </div>
          <div class="channel-icon-emblem">
            <svg viewBox="0 0 1024 1024" width="44" height="44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="wxGrad" x1="0" y1="0" x2="1024" y2="1024" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#28bd37"/>
                  <stop offset="100%" stop-color="#1ea82c"/>
                </linearGradient>
              </defs>
              <rect width="1024" height="1024" rx="240" fill="url(#wxGrad)"/>
              <path d="M743.8,284.1C724.6,270.2 692.5,246.3 663.6,233.4C634.7,220.4 600.8,213.0 573.7,207.8C549.1,203.2 529.3,202.1 504.3,203.2C477.2,204.5 439.9,209.2 414.4,214.9C391.9,220.0 374.0,227.8 354.1,237.0C334.2,246.1 316.1,254.9 297.3,268.8C277.3,283.5 253.3,304.3 236.7,323.4C220.6,341.9 210.1,360.8 199.7,380.8C189.3,400.8 180.0,418.7 175.7,441.2C171.3,463.7 172.2,490.7 174.3,513.4C176.5,535.9 181.3,554.7 188.4,574.6C195.4,594.6 204.0,612.8 215.8,630.9C227.6,649.0 243.0,665.4 258.0,681.0C273.0,696.5 296.1,710.1 304.1,722.5C310.2,731.8 306.7,742.5 304.9,753.5C302.7,767.3 292.9,793.4 290.8,804.0C289.9,808.2 292.6,815.7 292.6,815.7C292.6,815.7 300.4,817.4 304.1,815.4C317.5,808.0 357.3,780.7 371.7,772.4C377.4,769.1 382.4,766.1 389.0,766.4C406.6,767.2 449.5,775.0 475.2,776.7C498.5,778.3 518.8,778.1 540.1,776.8C561.5,775.4 583.1,772.5 601.0,768.9C617.4,765.5 630.3,762.4 645.4,755.3C667.3,745.0 703.6,727.7 729.4,708.6C755.1,689.5 779.2,665.2 796.7,643.0C813.2,622.2 823.4,601.0 832.3,578.3C841.3,555.5 846.5,526.5 849.4,509.3C851.3,497.8 850.6,488.6 849.2,477.0C847.1,460.9 843.7,432.9 837.4,414.4C831.2,396.5 811.9,368.5 811.9,368.5C811.9,368.5 802.9,368.4 798.9,370.9C733.2,410.1 493.3,560.4 425.8,599.0C418.2,603.3 409.3,601.1 401.6,597.6C393.9,594.0 385.0,587.2 380.5,577.9C367.7,551.1 334.5,465.9 326.3,439.8C324.5,434.1 328.6,427.5 332.5,424.4C336.4,421.2 343.9,418.3 349.0,421.4C364.2,430.5 406.6,467.3 421.7,477.7C426.6,481.2 432.5,482.4 437.6,482.8C442.5,483.2 446.6,482.1 451.2,480.0C507.6,454.2 713.9,357.6 769.4,330.8C773.1,329.0 776.1,325.3 777.4,322.6C778.6,320.2 778.6,317.2 776.8,315.2C771.1,308.6 757.0,293.7 743.8,284.1Z" fill="#ffffff"/>
            </svg>
          </div>
          <div class="channel-info">
            <div class="channel-name">微信支付</div>
            <span class="channel-tag tag-wechat">CNY</span>
          </div>
        </div>

        <div class="channel-card channel-alipay" :class="{ 'is-selected': currentChannel === 'alipay' }" @click="selectChannel('alipay')">
          <div class="channel-check-badge">
            <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="2.5 6 5 8.5 9.5 3.5"/></svg>
          </div>
          <div class="channel-icon-emblem">
            <svg viewBox="0 0 1024 1024" width="44" height="44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="wxBubble" x1="0" y1="0" x2="1024" y2="1024" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#f8fbff"/>
                  <stop offset="100%" stop-color="#ebf4ff"/>
                </linearGradient>
                <linearGradient id="aliGrad" x1="180" y1="140" x2="780" y2="860" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#0066ff"/>
                  <stop offset="65%" stop-color="#0b75ff"/>
                  <stop offset="100%" stop-color="#2b8eff"/>
                </linearGradient>
                <linearGradient id="aliRibbonGrad" x1="540" y1="620" x2="885" y2="790" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#1a80ff"/>
                  <stop offset="38%" stop-color="#69b1ff"/>
                  <stop offset="100%" stop-color="#c4e2ff"/>
                </linearGradient>
              </defs>
              <rect width="1024" height="1024" rx="236" fill="url(#wxBubble)"/>
              <rect x="18" y="18" width="988" height="988" rx="218" stroke="#1677ff" stroke-opacity="0.2" stroke-width="28"/>
              <path d="M247.3,296.2C244.4,299.9 241.3,307.2 239.4,313.9C237.4,320.8 236.1,332.0 235.8,336.9C235.6,339.1 236.3,341.0 237.6,342.7C239.0,344.4 241.0,346.8 243.6,346.8C278.5,347.6 408.2,346.7 442.7,347.4C444.5,347.4 446.3,348.8 446.4,350.6C446.8,361.5 444.9,411.6 444.9,411.6C444.9,411.6 333.8,411.5 309.6,413.0C305.9,413.2 303.9,416.7 302.3,420.0C299.9,424.9 296.6,435.4 295.3,442.0C294.2,447.9 294.5,454.9 295.0,458.7C295.3,461.0 296.1,464.1 298.4,464.1C344.6,465.2 520.4,464.4 566.8,464.8C568.5,464.8 570.4,465.5 571.3,466.6C572.3,467.8 573.0,469.8 572.6,471.7C569.7,485.6 560.7,528.3 554.3,548.5C549.3,564.3 535.1,590.4 535.1,590.4C535.1,590.4 489.8,570.6 471.9,563.6C457.1,557.7 444.3,553.4 430.0,549.1C415.7,544.8 402.5,541.3 387.7,538.4C373.0,535.4 359.3,532.7 343.1,531.6C325.8,530.4 300.6,530.5 285.6,531.6C274.4,532.4 265.7,534.8 254.9,538.0C244.1,541.2 233.3,544.6 222.1,550.4C210.7,556.3 197.7,564.8 188.1,572.5C179.1,579.9 172.2,587.3 165.6,595.8C159.1,604.4 154.1,612.6 149.5,622.9C144.9,633.2 140.7,645.8 138.4,656.4C136.3,666.4 136.2,676.0 136.2,684.8C136.3,693.1 136.7,699.7 138.7,707.8C141.0,717.6 145.2,732.0 150.1,742.6C155.0,753.2 161.3,761.8 167.7,769.9C174.0,778.1 179.5,784.2 187.6,790.5C197.2,798.1 210.8,807.9 224.4,814.4C238.1,820.9 255.8,825.8 267.7,829.0C277.2,831.5 284.9,832.4 294.8,832.9C306.3,833.6 323.8,833.5 335.3,832.9C345.3,832.4 353.0,831.4 362.7,829.5C376.1,827.0 396.0,823.7 414.1,818.2C432.5,812.6 451.7,805.6 470.9,796.4C490.1,787.1 509.1,776.0 527.0,763.8C545.0,751.7 576.7,724.7 576.7,724.7C576.7,724.7 659.0,776.4 681.2,789.8C690.3,795.2 697.6,799.1 707.3,803.4C719.8,808.9 741.8,817.9 755.3,822.4C766.3,826.1 775.2,828.4 786.7,829.9C798.5,831.4 815.4,831.8 824.9,831.5C831.4,831.2 836.8,830.7 842.5,827.6C848.6,824.4 855.6,818.6 861.0,812.2C866.4,805.8 871.1,798.7 874.5,789.9C878.7,779.1 883.3,761.0 885.5,748.3C887.6,736.6 888.4,723.0 887.5,715.2C886.9,709.9 883.1,705.3 880.4,702.4C877.9,699.7 874.8,698.5 871.2,698.1C862.4,697.1 843.8,698.6 828.6,696.6C808.6,693.8 775.5,687.5 753.2,682.1C732.9,677.2 716.5,671.9 697.8,664.9C679.0,642.9 640.8,640.8 642.9,640.8C642.9,640.8 666.6,594.5 675.3,573.9C683.4,554.8 689.2,537.6 694.1,519.4C699.1,501.3 702.6,483.4 704.6,467.0C706.6,451.3 706.2,431.6 706.1,423.1C706.1,420.7 705.7,418.5 704.0,416.8C702.2,415.0 698.9,412.5 695.4,412.4C671.4,411.7 586.2,413.6 562.4,412.7C559.4,412.6 556.1,410.4 555.8,407.4C554.6,397.2 555.2,363.0 555.6,352.9C555.7,350.9 558.2,347.9 558.2,347.9C558.2,347.9 713.9,348.2 747.8,346.6C752.4,346.4 755.3,342.5 757.9,338.6C760.5,334.6 762.2,328.8 763.4,323.0C764.7,316.7 765.7,306.6 765.5,301.7C765.4,298.8 764.0,295.9 762.3,294.3C760.6,292.7 757.9,292.2 755.3,292.1C720.6,291.7 558.4,291.8 558.4,291.8C558.4,291.8 555.7,288.8 555.6,286.7C555.1,270.4 556.7,212.7 555.6,196.4C555.4,193.5 552.4,191.6 549.5,191.0C544.9,190.0 536.1,189.8 528.7,190.7C516.6,192.3 491.0,195.9 478.4,200.1C468.7,203.2 459.8,210.3 454.6,215.2C450.7,219.0 448.7,223.6 448.1,229.0C446.8,241.2 447.5,276.3 446.8,287.0C446.6,289.1 445.7,291.9 443.6,292.0C411.3,292.8 289.6,291.4 256.2,292.1C252.7,292.2 249.5,293.4 247.3,296.2Z M183.8,688.1C183.0,684.0 183.1,680.5 184.0,676.3C185.1,670.9 188.0,661.9 190.3,656.4C192.4,651.5 194.8,648.0 198.0,643.8C201.3,639.5 204.5,634.9 209.4,631.3C216.9,626.0 232.1,616.8 242.0,612.2C250.8,608.2 261.0,606.1 267.7,604.5C272.6,603.4 276.6,602.9 281.6,602.9C293.0,602.9 318.1,602.3 334.7,604.3C351.0,606.3 363.4,609.7 379.0,615.0C397.1,621.1 423.2,631.9 441.2,640.5C457.5,648.4 484.7,665.9 484.7,665.9C484.7,665.9 476.9,676.9 471.7,682.4C465.8,688.5 458.4,695.6 450.0,702.0C439.2,710.2 422.2,722.5 408.6,730.4C395.1,738.2 380.0,744.4 369.5,748.7C361.5,752.0 355.0,753.9 346.5,755.8C335.8,758.2 319.8,762.2 306.3,762.9C292.8,763.6 277.1,761.3 267.0,759.9C259.7,758.9 254.1,757.3 247.1,754.9C239.6,752.2 230.3,748.6 223.1,744.3C215.9,740.0 209.0,733.8 204.5,729.8C201.2,726.9 198.9,724.0 196.5,720.6C194.1,717.2 192.0,714.0 190.4,709.8C188.2,704.3 184.9,693.8 183.8,688.1Z" fill="url(#aliGrad)" fill-rule="evenodd" clip-rule="evenodd"/>
              <path d="M684.8,543.7C684.8,543.7 671.0,578.1 663.3,594.2C655.7,610.2 644.8,630.2 639.9,638.0C638.7,639.8 636.3,640.4 634.3,639.8C627.8,637.9 602.0,626.7 602.0,626.7C602.0,626.7 599.5,629.1 599.3,630.9C598.7,635.3 597.7,647.4 598.3,652.6C598.7,656.3 600.5,658.8 602.6,661.8C608.8,670.7 622.9,689.8 635.0,705.1C648.2,721.8 668.1,746.3 680.4,760.2C689.3,770.4 696.7,778.0 707.0,787.0C717.4,796.0 741.6,813.6 741.6,813.6C741.6,813.6 721.2,806.8 710.3,801.8C697.7,795.9 682.3,788.2 667.4,779.2C644.8,765.5 593.6,731.2 577.4,721.7C575.7,720.7 573.6,721.9 572.0,723.2C565.7,728.5 540.2,752.5 540.2,752.5C540.2,752.5 565.9,731.0 572.3,726.3C574.0,725.0 576.0,723.6 577.8,724.6C596.3,735.4 654.3,774.3 681.2,789.7C700.2,800.6 718.0,808.6 736.3,815.5C754.6,822.4 773.7,827.4 788.7,830.1C801.4,832.5 814.9,832.3 824.6,831.5C832.5,830.9 839.2,829.7 845.9,825.7C852.6,821.8 858.8,815.6 864.1,808.2C869.5,800.6 874.4,791.5 877.6,781.1C881.5,768.6 885.8,746.9 887.1,734.5C888.1,725.0 888.0,714.2 885.5,708.0C883.2,702.6 878.1,699.4 872.2,698.3C862.5,696.3 844.0,698.7 828.3,696.6C808.2,693.9 775.9,687.7 753.9,682.4C733.8,677.5 717.8,672.1 699.1,665.4C680.5,658.7 653.7,647.6 644.2,642.8C642.3,641.8 642.1,639.0 642.9,637.1C647.1,628.0 661.8,604.6 668.9,588.7C676.0,573.0 684.8,543.7 684.8,543.7Z" fill="url(#aliRibbonGrad)"/>
            </svg>
          </div>
          <div class="channel-info">
            <div class="channel-name">支付宝</div>
            <span class="channel-tag tag-alipay">CNY</span>
          </div>
        </div>

        <div class="channel-card channel-bank" :class="{ 'is-selected': currentChannel === 'bank' }" @click="selectChannel('bank')">
          <div class="channel-check-badge">
            <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="2.5 6 5 8.5 9.5 3.5"/></svg>
          </div>
          <div class="channel-icon-emblem">
            <svg viewBox="0 0 48 48" width="44" height="44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="bankGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#6366f1"/>
                  <stop offset="100%" stop-color="#4338ca"/>
                </linearGradient>
                <linearGradient id="chipGrad" x1="13" y1="23" x2="21" y2="29" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#fde68a"/>
                  <stop offset="100%" stop-color="#f59e0b"/>
                </linearGradient>
              </defs>
              <rect width="48" height="48" rx="12" fill="url(#bankGrad)"/>
              <rect x="0.75" y="0.75" width="46.5" height="46.5" rx="11.25" stroke="#ffffff" stroke-opacity="0.24" stroke-width="1.5"/>
              <rect x="12" y="11.5" width="27" height="18" rx="3.5" fill="#ffffff" fill-opacity="0.22"/>
              <rect x="8.5" y="15.5" width="28.5" height="19.5" rx="3.8" fill="#ffffff"/>
              <path d="M8.5 19.3C8.5 17.2 10.2 15.5 12.3 15.5H33.2C35.3 15.5 37 17.2 37 19.3V20.5H8.5V19.3Z" fill="#c7d2fe"/>
              <rect x="12.5" y="23.2" width="7" height="5.2" rx="1.3" fill="url(#chipGrad)"/>
              <path d="M31.2 24.2C32.1 25.1 32.1 26.5 31.2 27.4" stroke="#4f46e5" stroke-width="1.6" stroke-linecap="round"/>
              <path d="M33.2 22.8C34.8 24.4 34.8 27.1 33.2 28.7" stroke="#4f46e5" stroke-width="1.6" stroke-linecap="round"/>
              <line x1="12.5" y1="31.5" x2="22" y2="31.5" stroke="#6366f1" stroke-width="1.8" stroke-linecap="round"/>
              <line x1="25.5" y1="31.5" x2="32.5" y2="31.5" stroke="#a5b4fc" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="channel-info">
            <div class="channel-name">银行卡流水</div>
            <span class="channel-tag tag-bank">多币种</span>
          </div>
        </div>

        <div class="channel-card channel-paypal" :class="{ 'is-selected': currentChannel === 'paypal' }" @click="selectChannel('paypal')">
          <div class="channel-check-badge">
            <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="2.5 6 5 8.5 9.5 3.5"/></svg>
          </div>
          <div class="channel-icon-emblem">
            <svg viewBox="0 0 48 48" width="44" height="44" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="ppGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#003087"/>
                  <stop offset="100%" stop-color="#0070ba"/>
                </linearGradient>
              </defs>
              <rect width="48" height="48" rx="12" fill="url(#ppGrad)"/>
              <rect x="0.75" y="0.75" width="46.5" height="46.5" rx="11.25" stroke="#ffffff" stroke-opacity="0.24" stroke-width="1.5"/>
              <path d="M22.2 16.5H30.4C34.5 16.5 36.8 18.7 36.1 22.8C35.3 27.4 32.1 29.8 27.6 29.8H24.2L22.8 37.5H17.8L21.3 17.3C21.4 16.8 21.8 16.5 22.2 16.5Z" fill="#38bdf8" fill-opacity="0.88"/>
              <path d="M17.2 11.5H26.8C31.2 11.5 33.8 13.9 33 18.4C32.1 23.4 28.6 26 23.6 26H19.6L17.9 35.5H12.2L16.1 12.4C16.2 11.9 16.7 11.5 17.2 11.5Z" fill="#ffffff"/>
              <path d="M21.1 17.8L19.6 26H23.6C27.8 26 31 24 32.4 20.1C31.5 17.6 29.5 16.5 26.5 16.5H22.2C21.6 16.5 21.2 17 21.1 17.8Z" fill="#e0f2fe"/>
            </svg>
          </div>
          <div class="channel-info">
            <div class="channel-name">PayPal</div>
            <span class="channel-tag tag-paypal">多币种</span>
          </div>
        </div>
      </div>

      <!-- 文件上传与就绪状态卡片 -->
      <div
        class="upload-zone-card"
        :class="{ 'is-dragover': isDragOver, 'has-file': !!uploadedFile }"
        @click="triggerFileSelect"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleFileDrop"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".xlsx,.xls,.csv"
          style="display: none;"
          @change="handleFileInputChange"
        />

        <div class="upload-left">
          <div class="upload-icon-circle">
            <el-icon v-if="!uploadedFile"><UploadFilled /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="upload-meta">
            <div v-if="!uploadedFile" class="upload-filename">
              <span>点击或拖拽账单文件至此上传</span>
            </div>
            <div v-else class="upload-filename">
              <span>{{ uploadedFile.name }}</span>
              <el-tag size="small" type="success" effect="plain">{{ formatFileSize(uploadedFile.size) }}</el-tag>
            </div>
            <div class="upload-sub">
              <span>支持微信、支付宝、银行流水导出的 .xlsx / .xls / .csv 格式账单文件</span>
              <span v-if="uploadedFile">· 点击卡片可重新选取或替换文件</span>
            </div>
          </div>
        </div>

        <div class="mapped-fields-row">
          <span class="field-chip">交易时间</span>
          <span class="field-chip">分类</span>
          <span class="field-chip">交易对方</span>
          <span class="field-chip">收/支</span>
          <span class="field-chip">金额</span>
        </div>
      </div>
    </div>

    <!-- ================= 步骤 2：统一分类确认 (核对护栏) ================= -->
    <div v-else-if="currentStep === 1" class="step-main-body">
      <!-- 步骤 2 顶部四维指标卡片 -->
      <div class="precheck-stat-grid">
        <div class="precheck-stat-card">
          <span class="stat-label">解析总数</span>
          <span class="stat-val num-font">{{ tableData.length }}</span>
        </div>
        <div class="precheck-stat-card">
          <span class="stat-label">已自动分类</span>
          <span class="stat-val num-font" style="color: #10b981;">{{ tableData.length - uncertainCount }}</span>
        </div>
        <div class="precheck-stat-card is-actionable" @click="handleStatCardUncertainClick">
          <span class="stat-label">待确认分类</span>
          <span class="stat-val num-font" :style="{ color: uncertainCount > 0 ? '#f59e0b' : 'var(--text-main)' }">
            {{ uncertainCount }}
          </span>
        </div>
        <div class="precheck-stat-card">
          <span class="stat-label">预计总额</span>
          <span class="stat-val num-font is-amount" style="color: var(--theme-light, #60a5fa);">¥ {{ estimatedTotalAmount }}</span>
        </div>
      </div>

      <!-- 分类胶囊过滤器 -->
      <div class="category-filter-bar">
        <button
          v-for="cat in categoryTabList"
          :key="cat.name"
          type="button"
          class="category-capsule-btn"
          :class="{
            'is-active': activeCategoryTab === cat.name,
            'is-warning': cat.name === '待确认'
          }"
          @click="activeCategoryTab = cat.name"
        >
          <span v-if="cat.name === '待确认'" class="warning-dot"></span>
          <span>{{ cat.name }} ({{ cat.count }})</span>
        </button>
      </div>

      <!-- 零抖动固定高度上下文操作条 -->
      <div class="table-context-dock" :class="{ 'is-batch-active': selectedRows.length > 0 }">
        <template v-if="selectedRows.length === 0">
          <span class="dock-instruction-text">
            勾选多笔流水可批量归类；单行分类框支持直接搜索或快速指定
          </span>
          <button
            v-if="uncertainCount > 0 && activeCategoryTab !== '待确认'"
            type="button"
            class="category-capsule-btn is-warning"
            @click="activeCategoryTab = '待确认'"
          >
            <span class="warning-dot"></span>
            <span>只看待确认 ({{ uncertainCount }})</span>
          </button>
        </template>
        <template v-else>
          <span class="dock-selected-text">
            已勾选 <strong class="num-font" style="color: var(--theme-light);">{{ selectedRows.length }}</strong> 笔流水
          </span>
          <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
            <el-select
              v-model="batchTargetCategory"
              size="small"
              filterable
              allow-create
              default-first-option
              clearable
              placeholder="选择批量分类"
              style="width: 160px;"
            >
              <el-option
                v-for="opt in bannerCategoryOptions"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>
            <el-button type="primary" size="small" :disabled="!batchTargetCategory" @click="applyBatchCategory">应用分类</el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
          </div>
        </template>
      </div>

      <!-- 护栏阻断常驻内嵌警示横幅 (位于 table-context-dock 与 modal-table-shell 之间) -->
      <transition name="guardrail-slide">
        <div v-if="guardrailBannerVisible && uncertainCount > 0" class="guardrail-inline-banner">
          <div class="guardrail-inline-left">
            <div class="guardrail-icon-box">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="guardrail-inline-text">
              <span class="guardrail-inline-title">
                尚有 <strong class="num-font" style="color: #f59e0b;">{{ uncertainCount }}</strong> 笔未分类账单：
              </span>
              <span class="guardrail-inline-desc">为保证统计精准，建议核对后再入账。您可直接归类或一键分流：</span>
            </div>
          </div>
          <div class="guardrail-inline-right">
            <div class="banner-quick-select-wrap">
              <el-select
                v-model="bannerTargetCategory"
                size="small"
                clearable
                placeholder="选择分类"
                style="width: 120px;"
              >
                <el-option
                  v-for="opt in bannerCategoryOptions"
                  :key="opt"
                  :label="opt"
                  :value="opt"
                />
              </el-select>
              <button
                type="button"
                class="guardrail-btn guardrail-btn-apply"
                :disabled="!bannerTargetCategory"
                @click="applyBannerCategory"
              >
                应用
              </button>
            </div>
            <button
              type="button"
              class="guardrail-btn guardrail-btn-primary"
              @click="autoAssignUncertainToOther"
            >
              将这些账单归为其他分类
            </button>
            <button type="button" class="guardrail-close-btn" @click="guardrailBannerVisible = false" title="关闭横幅">×</button>
          </div>
        </div>
      </transition>

      <!-- 流水核对表格容器 -->
      <div class="modal-table-shell">
        <el-table
          ref="step1TableRef"
          :data="filteredTableData"
          size="small"
          class="custom-bill-table is-compact-modal"
          max-height="300"
          style="width: 100%"
          :row-class-name="tableRowClassName"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="36" align="center" />
          <el-table-column label="交易时间" min-width="130" class-name="num-font">
            <template #default="{ row }">
              <span class="table-cell-time">{{ formatTime(row.time) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="135">
            <template #default="{ row }">
              <el-select
                v-model="row.category"
                size="small"
                filterable
                allow-create
                default-first-option
                style="width: 100%;"
                class="cat-inline-select"
                :class="{ 'is-uncertain': row.isUncertain || row.category === '待确认' }"
                :placeholder="row.type === '收入' ? '选择收入分类' : '选择支出分类'"
                @change="onCategoryChange(row)"
              >
                <el-option
                  v-for="c in getAvailableCategoriesForType(row.type)"
                  :key="c"
                  :label="c"
                  :value="c"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="交易对方 / 说明" min-width="190">
            <template #default="{ row }">
              <div class="cell-merchant-product is-double-line">
                <span class="cell-title">
                  <span>{{ getMerchantDisplay(row).title }}</span>
                  <span v-if="row.rawPayee && row.rawPayee !== row.payee" class="cleaned-tag" :title="'原始商户: ' + row.rawPayee">转译</span>
                </span>
                <template v-if="getMerchantDisplay(row).hasSub">
                  <span class="cell-divider">·</span>
                  <span class="cell-sub">{{ getMerchantDisplay(row).sub }}</span>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="支付方式" prop="payment" min-width="120">
            <template #default="{ row }">
              <div class="payment-cell-badge">
                <span class="payment-mini-icon" v-html="getPaymentMiniSvg(row.payment)"></span>
                <span style="overflow: hidden; text-overflow: ellipsis;">{{ row.payment || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额 (CNY)" min-width="100" align="right">
            <template #default="{ row }">
              <div class="num-font amount-main" :class="row.type === '支出' ? 'amount-expense' : 'amount-income'">
                {{ row.type === '支出' ? '-' : '+' }}¥{{ Number(row.amount || 0).toFixed(2) }}
              </div>
              <div v-if="row.isForeign && row.rawAmount" class="num-font amount-sub">{{ row.rawAmount }}</div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ================= 步骤 3：预检与排重 ================= -->
    <div v-else-if="currentStep === 2" class="step-main-body">
      <!-- 预检 4 项大盘指标卡片 -->
      <div class="precheck-stat-grid">
        <div class="precheck-stat-card">
          <span class="stat-label">解析笔数</span>
          <span class="stat-val num-font">{{ tableData.length }}</span>
        </div>
        <div class="precheck-stat-card">
          <span class="stat-label">预计入库</span>
          <span class="stat-val num-font" style="color: #10b981;">{{ finalImportCount }}</span>
        </div>
        <div class="precheck-stat-card">
          <span class="stat-label">疑似重复</span>
          <span class="stat-val num-font" :style="{ color: conflictCount > 0 ? '#f59e0b' : 'inherit' }">{{ conflictCount }}</span>
        </div>
        <div class="precheck-stat-card">
          <span class="stat-label">分类状态</span>
          <div class="stat-badge-success">
            <span>全部完成</span>
            <svg viewBox="0 0 16 16" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 8.5 6.5 12 13 4.5"></polyline>
            </svg>
          </div>
        </div>
      </div>

      <!-- 冲突去重策略条 -->
      <div v-if="conflictCount > 0" class="dedup-banner">
        <div class="dedup-banner-left">
          <div class="dedup-dot">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <span>识别到 <strong>{{ conflictCount }}</strong> 笔流水存在重复风险（同单号或跨渠道绑卡扣款）</span>
        </div>
        <el-radio-group v-model="duplicatePolicy" size="small">
          <el-radio-button value="auto_merge">跳过重复 (推荐)</el-radio-button>
          <el-radio-button value="keep_all">全部保留</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 冲突流水比对对比卡片 -->
      <div v-if="conflictCount > 0" class="conflict-compare-card">
        <div class="conflict-compare-header">
          <span style="display: inline-flex; align-items: center; gap: 6px;">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.85;">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            冲突流水比对详情 (共 {{ conflictCount }} 笔)
          </span>
          <span class="conflict-policy-hint">已按「{{ duplicatePolicy === 'auto_merge' ? '跳过重复' : '全部保留' }}」策略标记</span>
        </div>
        <div class="conflict-compare-list">
          <div v-for="(item, idx) in tableData.filter(r => r.isConflict)" :key="idx" class="conflict-compare-item">
            <div class="conflict-info-left">
              <span class="conflict-time">{{ formatTime(item.time) }}</span>
              <strong class="conflict-payee">{{ getMerchantDisplay(item).title }}</strong>
              <span v-if="getMerchantDisplay(item).hasSub" class="conflict-detail">{{ getMerchantDisplay(item).sub }}</span>
            </div>
            <div class="conflict-info-right">
              <span class="num-font conflict-amount" :class="item.type === '支出' ? 'amount-expense' : 'amount-income'">
                {{ item.type === '支出' ? '-' : '+' }}¥{{ Number(item.amount || 0).toFixed(2) }}
              </span>
              <span class="conflict-reason-badge">
                {{ item.conflictReason || '同渠道或绑卡重复' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 最终流水预检表（分类列为只读徽标） -->
      <div class="modal-table-shell">
        <el-table
          :data="tableData"
          size="small"
          class="custom-bill-table is-compact-modal"
          max-height="300"
          style="width: 100%"
          :row-class-name="step3TableRowClassName"
        >
          <el-table-column label="交易时间" min-width="130" class-name="num-font">
            <template #default="{ row }">
              <span class="table-cell-time">{{ formatTime(row.time) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="120">
            <template #default="{ row }">
              <div class="cat-readonly-cell">
                <span class="cat-readonly-tag">
                  <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.7;">
                    <rect x="2" y="2" width="8" height="8" rx="2"></rect>
                    <path d="M4 6h4M6 4v4"></path>
                  </svg>
                  {{ row.category || '未分类' }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="交易对方 / 说明" min-width="190">
            <template #default="{ row }">
              <div class="cell-merchant-product is-double-line">
                <span class="cell-title">
                  <span>{{ getMerchantDisplay(row).title }}</span>
                  <span v-if="row.rawPayee && row.rawPayee !== row.payee" class="cleaned-tag" :title="'原始商户: ' + row.rawPayee">转译</span>
                </span>
                <template v-if="getMerchantDisplay(row).hasSub">
                  <span class="cell-divider">·</span>
                  <span class="cell-sub">{{ getMerchantDisplay(row).sub }}</span>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="支付方式" prop="payment" min-width="120">
            <template #default="{ row }">
              <div class="payment-cell-badge">
                <span class="payment-mini-icon" v-html="getPaymentMiniSvg(row.payment)"></span>
                <span style="overflow: hidden; text-overflow: ellipsis;">{{ row.payment || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额 (CNY)" min-width="100" align="right">
            <template #default="{ row }">
              <div class="num-font amount-main" :class="row.type === '支出' ? 'amount-expense' : 'amount-income'">
                {{ row.type === '支出' ? '-' : '+' }}¥{{ Number(row.amount || 0).toFixed(2) }}
              </div>
              <div v-if="row.isForeign && row.rawAmount" class="num-font amount-sub">{{ row.rawAmount }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="95" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="row.isConflict" :content="row.conflictReason || '疑似与历史账单或跨渠道绑卡重复'" placement="top">
                <span :class="duplicatePolicy === 'auto_merge' ? 'custom-table-tag tag-status-conflict-skip' : 'custom-table-tag tag-status-conflict-keep'">
                  {{ duplicatePolicy === 'auto_merge' ? '重复·跳过' : '重复·保留' }}
                </span>
              </el-tooltip>
              <span v-else-if="row.isForeign" class="custom-table-tag tag-status-foreign">外币折算</span>
              <span v-else class="custom-table-tag tag-status-normal">正常入库</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ================= 步骤 4：完成入库 ================= -->
    <div v-else-if="currentStep === 3" class="step-main-body finish-wrap">
      <div class="finish-icon-circle"><el-icon><Check /></el-icon></div>
      <h3 class="finish-title">账单导入完成</h3>
      <p class="finish-desc">{{ getChannelName(currentChannel) }}流水已成功记入账本，重复记录已按所选策略处理。</p>

      <div class="report-card-grid">
        <div class="report-card-item">
          <div class="stat-label" style="margin-bottom: 4px;">新增入库</div>
          <div class="stat-val num-font" style="color: #10b981;">{{ finalImportCount }} 笔</div>
        </div>
        <div class="report-card-item">
          <div class="stat-label" style="margin-bottom: 4px;">去重过滤</div>
          <div class="stat-val num-font" style="color: #f59e0b;">{{ finalSkippedCount }} 笔</div>
        </div>
        <div class="report-card-item">
          <div class="stat-label" style="margin-bottom: 4px;">外币折算</div>
          <div class="stat-val num-font" style="color: #06b6d4;">{{ foreignCount }} 笔</div>
        </div>
        <div class="report-card-item">
          <div class="stat-label" style="margin-bottom: 4px;">入库总额</div>
          <div class="stat-val num-font" style="color: var(--theme-light, #60a5fa);">¥ {{ finalTotalAmount }}</div>
        </div>
      </div>
    </div>

    <!-- 底部操作按钮栏 -->
    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
        <div :style="{ fontSize: 'calc(13px * var(--app-font-scale, 1))', color: 'var(--text-sub)' }">
          <span v-if="currentStep === 0">当前渠道：<strong style="color: var(--text-main);">{{ getChannelName(currentChannel) }}</strong></span>
          <span v-else-if="currentStep === 1">
            待确认：<strong class="num-font" :style="{ color: uncertainCount > 0 ? '#f59e0b' : '#10b981' }">{{ uncertainCount }} 笔</strong>
          </span>
          <span v-else-if="currentStep === 2">
            预计入库金额：<strong class="num-font" style="color: var(--theme-light);">¥ {{ finalTotalAmount }}</strong>
          </span>
          <span v-else-if="currentStep === 3">
            已成功同步至主账本
          </span>
        </div>

        <div style="display: flex; gap: 10px;">
          <!-- 步骤 1 底部按钮 -->
          <template v-if="currentStep === 0">
            <el-button class="btn-action btn-toolbar-reset" @click="dialogVisible = false">取消</el-button>
            <el-button class="btn-action btn-toolbar-search" type="primary" :loading="isPrechecking" @click="handleStep0Next">下一步</el-button>
          </template>

          <!-- 步骤 2 底部按钮 -->
          <template v-else-if="currentStep === 1">
            <el-button class="btn-action btn-toolbar-reset" @click="goToStep(0)">上一步</el-button>
            <el-button class="btn-action btn-toolbar-search" type="primary" @click="handleStep1Next">下一步</el-button>
          </template>

          <!-- 步骤 3 底部按钮 -->
          <template v-else-if="currentStep === 2">
            <el-button class="btn-action btn-toolbar-reset" @click="goToStep(1)">上一步</el-button>
            <el-button class="btn-action btn-toolbar-create" type="primary" :loading="isSubmitting" @click="executeImport">
              确认入库 ({{ finalImportCount }} 笔)
            </el-button>
          </template>

          <!-- 步骤 4 底部按钮 -->
          <template v-else-if="currentStep === 3">
            <el-button class="btn-action btn-toolbar-search" type="primary" @click="finishAndClose">完成并查看账单</el-button>
          </template>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue';
import { Check, UploadFilled, Document, WarningFilled } from '@element-plus/icons-vue';
import { ElMessage, ElNotification } from 'element-plus';
import { request } from '@/utils/request.js';
import dayjs from 'dayjs';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:visible', 'success']);

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
});

// 步骤与状态
const currentStep = ref(0);
const currentChannel = ref('wechat');
const isFinishStep = computed(() => currentStep.value === 3);
const isPrechecking = ref(false);
const isSubmitting = ref(false);

// 文件上传
const fileInputRef = ref(null);
const uploadedFile = ref(null);
const isDragOver = ref(false);

// 表格数据与预检指标
const tableData = ref([]);
const activeCategoryTab = ref('全部');
const selectedRows = ref([]);
const step1TableRef = ref(null);

// 护栏横幅与批量指定
const guardrailBannerVisible = ref(false);
const bannerTargetCategory = ref('');
const batchTargetCategory = ref('');
const duplicatePolicy = ref('auto_merge'); // 'auto_merge' | 'keep_all'

// 分类词典
const EXPENSE_CATEGORIES = ['餐饮美食', '日常购物', '交通出行', '生活缴费', '休闲娱乐', '数码科技', '医疗健康', '金融服务', '其他支出'];
const INCOME_CATEGORIES = ['薪资报酬', '副业外快', '投资理财', '红包转账', '退款返还', '补贴奖励', '其他收入'];

const getAvailableCategoriesForType = (type) => {
  return type === '收入' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
};

const bannerCategoryOptions = computed(() => {
  const uncertainRows = tableData.value.filter(r => r.isUncertain || r.category === '待确认');
  const hasExpense = uncertainRows.some(r => r.type === '支出');
  const hasIncome = uncertainRows.some(r => r.type === '收入');
  if (hasExpense && !hasIncome) return EXPENSE_CATEGORIES;
  if (!hasExpense && hasIncome) return INCOME_CATEGORIES;
  return [...new Set([...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES])];
});

const uncertainCount = computed(() => {
  return tableData.value.filter(r => r.isUncertain || r.category === '待确认').length;
});

const conflictCount = computed(() => {
  return tableData.value.filter(r => r.isConflict).length;
});

const foreignCount = computed(() => {
  return tableData.value.filter(r => r.isForeign).length;
});

const finalImportCount = computed(() => {
  if (duplicatePolicy.value === 'auto_merge') {
    return tableData.value.filter(r => !r.isConflict).length;
  }
  return tableData.value.length;
});

const finalSkippedCount = computed(() => {
  if (duplicatePolicy.value === 'auto_merge') {
    return tableData.value.filter(r => r.isConflict).length;
  }
  return 0;
});

const finalTotalAmount = computed(() => {
  const targetRows = duplicatePolicy.value === 'auto_merge'
    ? tableData.value.filter(r => !r.isConflict)
    : tableData.value;
  const sum = targetRows.reduce((acc, cur) => acc + Number(cur.amount || 0), 0);
  return sum.toFixed(2);
});

const estimatedTotalAmount = computed(() => {
  const sum = tableData.value.reduce((acc, cur) => acc + Number(cur.amount || 0), 0);
  return sum.toFixed(2);
});

const handleStatCardUncertainClick = () => {
  if (uncertainCount.value > 0) {
    activeCategoryTab.value = '待确认';
  }
};

const categoryTabList = computed(() => {
  const map = {};
  tableData.value.forEach(row => {
    const cat = row.category || '待确认';
    map[cat] = (map[cat] || 0) + 1;
  });
  const list = [{ name: '全部', count: tableData.value.length }];
  if (uncertainCount.value > 0) {
    list.push({ name: '待确认', count: uncertainCount.value });
  }
  Object.keys(map).forEach(cat => {
    if (cat !== '待确认') {
      list.push({ name: cat, count: map[cat] });
    }
  });
  return list;
});

const filteredTableData = computed(() => {
  let list = tableData.value;
  if (activeCategoryTab.value === '全部') {
    return list;
  }
  if (activeCategoryTab.value === '待确认') {
    return list.filter(r => r.isUncertain || r.category === '待确认');
  }
  return list.filter(r => r.category === activeCategoryTab.value);
});

// 渠道与文件方法
const selectChannel = (channel) => {
  currentChannel.value = channel;
  resetBatchCategoryState();
};

const getChannelName = (ch) => {
  const names = { wechat: '微信支付', alipay: '支付宝', bank: '银行卡流水', paypal: 'PayPal' };
  return names[ch] || '所选渠道';
};

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
};

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-';
};

const getMerchantDisplay = (row) => {
  if (!row) return { title: '-', sub: '', hasSub: false };
  let payee = (row.payee || '').trim();
  let detail = (row.detail || '').trim();

  // 过滤无效纯符号的商品说明
  if (detail === '/' || detail === '-' || detail === '.' || detail === '无' || detail === '无说明' || detail === '无商品说明') {
    detail = '';
  }

  const isInvalidPayee = !payee || payee === '/' || payee === '-' || payee === '.' || payee === '未知商户' || payee === '未知' || payee === '其他';

  // 1. 当收款方为空或无效符号时，智能从商品说明/交易摘要中提取商户名与业务子项
  if (isInvalidPayee) {
    if (!detail) {
      return { title: '其他收支', sub: '', hasSub: false };
    }

    // 匹配如 "网易BUFF出售-印花|TYLOO (全息) | 2020 RMR" -> brand: "网易BUFF", sub: "出售 印花|TYLOO..."
    const match = detail.match(/^([a-zA-Z0-9\u4e00-\u9fa5]+?)(出售|代发|充值|退款|快捷支付|支付|App|商城|平台)?\s*[-_—:|/]\s*(.*)$/);
    if (match && match[1].trim().length >= 2) {
      const brand = match[1].trim();
      const action = match[2] ? match[2].trim() : '';
      const rest = match[3] ? match[3].trim() : '';
      const sub = action ? `${action} ${rest}`.trim() : rest;
      return { title: brand, sub, hasSub: Boolean(sub) };
    }

    // 常见分隔符分割商户与子项
    const delIdx = detail.search(/[-_—:|/]/);
    if (delIdx > 1 && delIdx < detail.length - 1) {
      const title = detail.substring(0, delIdx).trim();
      const sub = detail.substring(delIdx + 1).trim();
      return { title, sub, hasSub: Boolean(sub) };
    }

    // 空格分割商户与子项（如 "海底捞火锅 3号桌消费"）
    const spaceIdx = detail.indexOf(' ');
    if (spaceIdx >= 2 && spaceIdx <= 12) {
      const title = detail.substring(0, spaceIdx).trim();
      const sub = detail.substring(spaceIdx + 1).trim();
      return { title, sub, hasSub: Boolean(sub) };
    }

    return { title: detail, sub: '', hasSub: false };
  }

  // 2. 当商户与说明完全一致或说明为空时
  if (!detail || detail === payee) {
    // 智能分解类似 "团app短视频-现金激励-微信零钱" 中的品牌与细项
    const match = payee.match(/^([^\-_—:|/]+?)[\-_—:|/]\s*(.+)$/);
    if (match && match[1].trim().length >= 2 && !match[1].startsWith('商户')) {
      return { title: match[1].trim(), sub: match[2].trim(), hasSub: true };
    }
    return { title: payee, sub: '', hasSub: false };
  }

  return { title: payee, sub: detail, hasSub: true };
};

const resetBatchCategoryState = () => {
  bannerTargetCategory.value = '';
  batchTargetCategory.value = '';
  selectedRows.value = [];
  step1TableRef.value?.clearSelection();
};

const triggerFileSelect = () => {
  resetBatchCategoryState();
  fileInputRef.value?.click();
};

const handleFileInputChange = (e) => {
  const file = e.target.files?.[0];
  if (file) {
    uploadedFile.value = file;
    resetBatchCategoryState();
  }
};

const handleFileDrop = (e) => {
  isDragOver.value = false;
  const file = e.dataTransfer.files?.[0];
  if (file) {
    uploadedFile.value = file;
    resetBatchCategoryState();
  }
};

// 监听弹窗显隐，确保打开或关闭时完全重置批量分类选择状态
watch(() => props.visible, (val) => {
  resetBatchCategoryState();
  if (!val) {
    guardrailBannerVisible.value = false;
  }
});

// 步骤跳转与接口调用
const goToStep = (step) => {
  currentStep.value = step;
  resetBatchCategoryState();
};

// 步骤变更时确保批量选择状态自动清空
watch(() => currentStep.value, () => {
  resetBatchCategoryState();
});

const handleStep0Next = async () => {
  resetBatchCategoryState();
  if (!uploadedFile.value) {
    ElMessage.warning('请先点击卡片或拖入要导入的账单文件');
    return;
  }

  isPrechecking.value = true;
  try {
    const formData = new FormData();
    formData.append('file', uploadedFile.value);
    formData.append('channel', currentChannel.value);

    const res = await request.post('/bills/import/precheck', formData);

    if (res.code === 200) {
      const vo = res.data;
      tableData.value = (vo.items || []).map(item => ({
        ...item,
        isUncertain: item.isUncertain || item.category === '待确认'
      }));

      if (tableData.value.length === 0) {
        ElMessage.warning('未能从文件中解析到有效账单数据，请检查文件格式');
        return;
      }

      if (vo.isChannelCorrected && vo.channel) {
        currentChannel.value = vo.channel;
        ElNotification({
          title: '智能渠道纠偏',
          message: vo.correctionNotice || `检测到该文件为【${getChannelName(vo.channel)}】账单，已自动切换解析引擎`,
          type: 'info',
          duration: 4500
        });
      }

      ElMessage.success(`预检完成：共解析 ${vo.totalCount} 笔，自动分类 ${vo.autoCategorizedCount} 笔`);
      goToStep(1);
    } else {
      ElMessage.error(res.msg || '账单预检解析失败');
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '网络异常或服务无响应，预检失败');
  } finally {
    isPrechecking.value = false;
  }
};

const handleStep1Next = () => {
  if (uncertainCount.value > 0) {
    guardrailBannerVisible.value = true;
    nextTick(() => {
      const firstUncertIdx = filteredTableData.value.findIndex(r => r.isUncertain || r.category === '待确认');
      if (firstUncertIdx !== -1 && step1TableRef.value) {
        const rows = step1TableRef.value.$el.querySelectorAll('.el-table__row');
        if (rows[firstUncertIdx]) {
          rows[firstUncertIdx].scrollIntoView({ behavior: 'smooth', block: 'center' });
          rows[firstUncertIdx].classList.add('table-row-shaking');
          setTimeout(() => rows[firstUncertIdx]?.classList.remove('table-row-shaking'), 800);
        }
      }
    });
    ElMessage.warning(`尚有 ${uncertainCount.value} 笔流水未确认分类，请核对后再继续`);
    return;
  }
  goToStep(2);
};

const onCategoryChange = (row) => {
  if (row.category && row.category !== '待确认') {
    row.isUncertain = false;
  }
};

const autoAssignUncertainToOther = () => {
  tableData.value.forEach(row => {
    if (row.isUncertain || row.category === '待确认') {
      row.category = row.type === '收入' ? '其他收入' : '其他支出';
      row.isUncertain = false;
    }
  });
  guardrailBannerVisible.value = false;
  ElMessage.success('已自动将所有待确认账单归入其他收支');
};

const applyBannerCategory = () => {
  if (!bannerTargetCategory.value) return;
  tableData.value.forEach(row => {
    if (row.isUncertain || row.category === '待确认') {
      row.category = bannerTargetCategory.value;
      row.isUncertain = false;
    }
  });
  guardrailBannerVisible.value = false;
  bannerTargetCategory.value = '';
  ElMessage.success('批量应用分类成功');
};

const applyBatchCategory = () => {
  if (!batchTargetCategory.value || selectedRows.value.length === 0) return;
  selectedRows.value.forEach(row => {
    row.category = batchTargetCategory.value;
    row.isUncertain = false;
  });
  batchTargetCategory.value = '';
  clearSelection();
  ElMessage.success('批量修改分类成功');
};

const clearSelection = () => {
  step1TableRef.value?.clearSelection();
  selectedRows.value = [];
};

const handleSelectionChange = (selection) => {
  selectedRows.value = selection;
};

const tableRowClassName = ({ row }) => {
  if (row.isUncertain || row.category === '待确认') {
    return 'table-row-attention';
  }
  return '';
};

const step3TableRowClassName = ({ row }) => {
  if (row.isConflict) {
    return 'table-row-conflict';
  }
  return '';
};

// 步骤 3 提交落库
const executeImport = async () => {
  isSubmitting.value = true;
  try {
    const confirmDTO = {
      items: tableData.value,
      channel: currentChannel.value,
      duplicateAction: duplicatePolicy.value === 'auto_merge' ? 'auto_merge' : 'keep_all'
    };

    const res = await request.post('/bills/import/confirm', confirmDTO);
    if (res.code === 200) {
      goToStep(3);
      emit('success');
    } else {
      ElMessage.error(res.msg || '账单落库入账失败');
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '系统异常，入库失败');
  } finally {
    isSubmitting.value = false;
  }
};

const finishAndClose = () => {
  dialogVisible.value = false;
};

const handleClosed = () => {
  currentStep.value = 0;
  uploadedFile.value = null;
  tableData.value = [];
  guardrailBannerVisible.value = false;
  resetBatchCategoryState();
};

const miniSvgMap = {
  wechat: `<svg viewBox="0 0 1024 1024" width="18" height="18" fill="none"><rect width="1024" height="1024" rx="240" fill="#22b331"/><path d="M743.8,284.1C724.6,270.2 692.5,246.3 663.6,233.4C634.7,220.4 600.8,213.0 573.7,207.8C549.1,203.2 529.3,202.1 504.3,203.2C477.2,204.5 439.9,209.2 414.4,214.9C391.9,220.0 374.0,227.8 354.1,237.0C334.2,246.1 316.1,254.9 297.3,268.8C277.3,283.5 253.3,304.3 236.7,323.4C220.6,341.9 210.1,360.8 199.7,380.8C189.3,400.8 180.0,418.7 175.7,441.2C171.3,463.7 172.2,490.7 174.3,513.4C176.5,535.9 181.3,554.7 188.4,574.6C195.4,594.6 204.0,612.8 215.8,630.9C227.6,649.0 243.0,665.4 258.0,681.0C273.0,696.5 296.1,710.1 304.1,722.5C310.2,731.8 306.7,742.5 304.9,753.5C302.7,767.3 292.9,793.4 290.8,804.0C289.9,808.2 292.6,815.7 292.6,815.7C292.6,815.7 300.4,817.4 304.1,815.4C317.5,808.0 357.3,780.7 371.7,772.4C377.4,769.1 382.4,766.1 389.0,766.4C406.6,767.2 449.5,775.0 475.2,776.7C498.5,778.3 518.8,778.1 540.1,776.8C561.5,775.4 583.1,772.5 601.0,768.9C617.4,765.5 630.3,762.4 645.4,755.3C667.3,745.0 703.6,727.7 729.4,708.6C755.1,689.5 779.2,665.2 796.7,643.0C813.2,622.2 823.4,601.0 832.3,578.3C841.3,555.5 846.5,526.5 849.4,509.3C851.3,497.8 850.6,488.6 849.2,477.0C847.1,460.9 843.7,432.9 837.4,414.4C831.2,396.5 811.9,368.5 811.9,368.5C811.9,368.5 802.9,368.4 798.9,370.9C733.2,410.1 493.3,560.4 425.8,599.0C418.2,603.3 409.3,601.1 401.6,597.6C393.9,594.0 385.0,587.2 380.5,577.9C367.7,551.1 334.5,465.9 326.3,439.8C324.5,434.1 328.6,427.5 332.5,424.4C336.4,421.2 343.9,418.3 349.0,421.4C364.2,430.5 406.6,467.3 421.7,477.7C426.6,481.2 432.5,482.4 437.6,482.8C442.5,483.2 446.6,482.1 451.2,480.0C507.6,454.2 713.9,357.6 769.4,330.8C773.1,329.0 776.1,325.3 777.4,322.6C778.6,320.2 778.6,317.2 776.8,315.2C771.1,308.6 757.0,293.7 743.8,284.1Z" fill="#ffffff"/></svg>`,
  alipay: `<svg viewBox="0 0 1024 1024" width="18" height="18" fill="none"><rect width="1024" height="1024" rx="240" fill="#f0f7ff" stroke="#1677ff" stroke-opacity="0.28" stroke-width="48"/><path d="M247.3,296.2C244.4,299.9 241.3,307.2 239.4,313.9C237.4,320.8 236.1,332.0 235.8,336.9C235.6,339.1 236.3,341.0 237.6,342.7C239.0,344.4 241.0,346.8 243.6,346.8C278.5,347.6 408.2,346.7 442.7,347.4C444.5,347.4 446.3,348.8 446.4,350.6C446.8,361.5 444.9,411.6 444.9,411.6C444.9,411.6 333.8,411.5 309.6,413.0C305.9,413.2 303.9,416.7 302.3,420.0C299.9,424.9 296.6,435.4 295.3,442.0C294.2,447.9 294.5,454.9 295.0,458.7C295.3,461.0 296.1,464.1 298.4,464.1C344.6,465.2 520.4,464.4 566.8,464.8C568.5,464.8 570.4,465.5 571.3,466.6C572.3,467.8 573.0,469.8 572.6,471.7C569.7,485.6 560.7,528.3 554.3,548.5C549.3,564.3 535.1,590.4 535.1,590.4C535.1,590.4 489.8,570.6 471.9,563.6C457.1,557.7 444.3,553.4 430.0,549.1C415.7,544.8 402.5,541.3 387.7,538.4C373.0,535.4 359.3,532.7 343.1,531.6C325.8,530.4 300.6,530.5 285.6,531.6C274.4,532.4 265.7,534.8 254.9,538.0C244.1,541.2 233.3,544.6 222.1,550.4C210.7,556.3 197.7,564.8 188.1,572.5C179.1,579.9 172.2,587.3 165.6,595.8C159.1,604.4 154.1,612.6 149.5,622.9C144.9,633.2 140.7,645.8 138.4,656.4C136.3,666.4 136.2,676.0 136.2,684.8C136.3,693.1 136.7,699.7 138.7,707.8C141.0,717.6 145.2,732.0 150.1,742.6C155.0,753.2 161.3,761.8 167.7,769.9C174.0,778.1 179.5,784.2 187.6,790.5C197.2,798.1 210.8,807.9 224.4,814.4C238.1,820.9 255.8,825.8 267.7,829.0C277.2,831.5 284.9,832.4 294.8,832.9C306.3,833.6 323.8,833.5 335.3,832.9C345.3,832.4 353.0,831.4 362.7,829.5C376.1,827.0 396.0,823.7 414.1,818.2C432.5,812.6 451.7,805.6 470.9,796.4C490.1,787.1 509.1,776.0 527.0,763.8C545.0,751.7 576.7,724.7 576.7,724.7C576.7,724.7 659.0,776.4 681.2,789.8C690.3,795.2 697.6,799.1 707.3,803.4C719.8,808.9 741.8,817.9 755.3,822.4C766.3,826.1 775.2,828.4 786.7,829.9C798.5,831.4 815.4,831.8 824.9,831.5C831.4,831.2 836.8,830.7 842.5,827.6C848.6,824.4 855.6,818.6 861.0,812.2C866.4,805.8 871.1,798.7 874.5,789.9C878.7,779.1 883.3,761.0 885.5,748.3C887.6,736.6 888.4,723.0 887.5,715.2C886.9,709.9 883.1,705.3 880.4,702.4C877.9,699.7 874.8,698.5 871.2,698.1C862.4,697.1 843.8,698.6 828.6,696.6C808.6,693.8 775.5,687.5 753.2,682.1C732.9,677.2 716.5,671.9 697.8,664.9C679.0,642.9 640.8,640.8 642.9,640.8C642.9,640.8 666.6,594.5 675.3,573.9C683.4,554.8 689.2,537.6 694.1,519.4C699.1,501.3 702.6,483.4 704.6,467.0C706.6,451.3 706.2,431.6 706.1,423.1C706.1,420.7 705.7,418.5 704.0,416.8C702.2,415.0 698.9,412.5 695.4,412.4C671.4,411.7 586.2,413.6 562.4,412.7C559.4,412.6 556.1,410.4 555.8,407.4C554.6,397.2 555.2,363.0 555.6,352.9C555.7,350.9 558.2,347.9 558.2,347.9C558.2,347.9 713.9,348.2 747.8,346.6C752.4,346.4 755.3,342.5 757.9,338.6C760.5,334.6 762.2,328.8 763.4,323.0C764.7,316.7 765.7,306.6 765.5,301.7C765.4,298.8 764.0,295.9 762.3,294.3C760.6,292.7 757.9,292.2 755.3,292.1C720.6,291.7 558.4,291.8 558.4,291.8C558.4,291.8 555.7,288.8 555.6,286.7C555.1,270.4 556.7,212.7 555.6,196.4C555.4,193.5 552.4,191.6 549.5,191.0C544.9,190.0 536.1,189.8 528.7,190.7C516.6,192.3 491.0,195.9 478.4,200.1C468.7,203.2 459.8,210.3 454.6,215.2C450.7,219.0 448.7,223.6 448.1,229.0C446.8,241.2 447.5,276.3 446.8,287.0C446.6,289.1 445.7,291.9 443.6,292.0C411.3,292.8 289.6,291.4 256.2,292.1C252.7,292.2 249.5,293.4 247.3,296.2Z M183.8,688.1C183.0,684.0 183.1,680.5 184.0,676.3C185.1,670.9 188.0,661.9 190.3,656.4C192.4,651.5 194.8,648.0 198.0,643.8C201.3,639.5 204.5,634.9 209.4,631.3C216.9,626.0 232.1,616.8 242.0,612.2C250.8,608.2 261.0,606.1 267.7,604.5C272.6,603.4 276.6,602.9 281.6,602.9C293.0,602.9 318.1,602.3 334.7,604.3C351.0,606.3 363.4,609.7 379.0,615.0C397.1,621.1 423.2,631.9 441.2,640.5C457.5,648.4 484.7,665.9 484.7,665.9C484.7,665.9 476.9,676.9 471.7,682.4C465.8,688.5 458.4,695.6 450.0,702.0C439.2,710.2 422.2,722.5 408.6,730.4C395.1,738.2 380.0,744.4 369.5,748.7C361.5,752.0 355.0,753.9 346.5,755.8C335.8,758.2 319.8,762.2 306.3,762.9C292.8,763.6 277.1,761.3 267.0,759.9C259.7,758.9 254.1,757.3 247.1,754.9C239.6,752.2 230.3,748.6 223.1,744.3C215.9,740.0 209.0,733.8 204.5,729.8C201.2,726.9 198.9,724.0 196.5,720.6C194.1,717.2 192.0,714.0 190.4,709.8C188.2,704.3 184.9,693.8 183.8,688.1Z" fill="#0068ff" fill-rule="evenodd" clip-rule="evenodd"/><path d="M684.8,543.7C684.8,543.7 671.0,578.1 663.3,594.2C655.7,610.2 644.8,630.2 639.9,638.0C638.7,639.8 636.3,640.4 634.3,639.8C627.8,637.9 602.0,626.7 602.0,626.7C602.0,626.7 599.5,629.1 599.3,630.9C598.7,635.3 597.7,647.4 598.3,652.6C598.7,656.3 600.5,658.8 602.6,661.8C608.8,670.7 622.9,689.8 635.0,705.1C648.2,721.8 668.1,746.3 680.4,760.2C689.3,770.4 696.7,778.0 707.0,787.0C717.4,796.0 741.6,813.6 741.6,813.6C741.6,813.6 721.2,806.8 710.3,801.8C697.7,795.9 682.3,788.2 667.4,779.2C644.8,765.5 593.6,731.2 577.4,721.7C575.7,720.7 573.6,721.9 572.0,723.2C565.7,728.5 540.2,752.5 540.2,752.5C540.2,752.5 565.9,731.0 572.3,726.3C574.0,725.0 576.0,723.6 577.8,724.6C596.3,735.4 654.3,774.3 681.2,789.7C700.2,800.6 718.0,808.6 736.3,815.5C754.6,822.4 773.7,827.4 788.7,830.1C801.4,832.5 814.9,832.3 824.6,831.5C832.5,830.9 839.2,829.7 845.9,825.7C852.6,821.8 858.8,815.6 864.1,808.2C869.5,800.6 874.4,791.5 877.6,781.1C881.5,768.6 885.8,746.9 887.1,734.5C888.1,725.0 888.0,714.2 885.5,708.0C883.2,702.6 878.1,699.4 872.2,698.3C862.5,696.3 844.0,698.7 828.3,696.6C808.2,693.9 775.9,687.7 753.9,682.4C733.8,677.5 717.8,672.1 699.1,665.4C680.5,658.7 653.7,647.6 644.2,642.8C642.3,641.8 642.1,639.0 642.9,637.1C647.1,628.0 661.8,604.6 668.9,588.7C676.0,573.0 684.8,543.7 684.8,543.7Z" fill="#78bcff"/></svg>`,
  bank: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none"><rect width="24" height="24" rx="6" fill="#6366f1"/><rect x="4" y="7" width="16" height="10.5" rx="2" fill="#fff"/><rect x="4" y="8.8" width="16" height="2.2" fill="#c7d2fe"/><rect x="6.2" y="12.5" width="3.8" height="2.8" rx="0.6" fill="#f59e0b"/></svg>`,
  paypal: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none"><rect width="24" height="24" rx="6" fill="#003087"/><path d="M11.2 8.2H15.2C17.2 8.2 18.4 9.3 18 11.4C17.6 13.7 16 14.9 13.8 14.9H12.1L11.4 18.8H8.9L10.7 8.6C10.7 8.4 10.9 8.2 11.2 8.2Z" fill="#38bdf8"/><path d="M8.6 5.8H13.4C15.6 5.8 16.9 7 16.5 9.2C16 11.7 14.3 13 11.8 13H9.8L8.9 17.8H6.1L8.1 6.2C8.1 6 8.4 5.8 8.6 5.8Z" fill="#fff"/></svg>`
};

const getPaymentMiniSvg = (paymentText) => {
  const text = String(paymentText || '').toLowerCase();
  if (text.includes('微信') || text.includes('零钱')) return miniSvgMap.wechat;
  if (text.includes('支付宝') || text.includes('花呗') || text.includes('余额宝')) return miniSvgMap.alipay;
  if (text.includes('paypal')) return miniSvgMap.paypal;
  return miniSvgMap.bank;
};
</script>

<style scoped>
.num-font {
  font-family: var(--font-mono, monospace);
  font-variant-numeric: tabular-nums;
}

/* ================= 1. 弹窗本体与外壳 ================= */
.import-dialog-wrap :deep(.el-dialog) {
  border-radius: 16px !important;
  overflow: hidden !important;
  background: var(--bg-surface, var(--table-row-bg, #ffffff)) !important;
  border: 1px solid var(--border-strong, var(--table-border-color, rgba(148, 163, 184, 0.2))) !important;
  box-shadow: 0 28px 64px -12px rgba(0, 0, 0, 0.6) !important;
}

.import-dialog-wrap :deep(.el-dialog__header) {
  margin: 0 !important;
  padding: 14px 44px 14px 22px !important;
  border-bottom: 1px solid var(--border-subtle, var(--table-divider, rgba(15, 23, 42, 0.08))) !important;
  background: var(--bg-surface, var(--table-row-bg, #ffffff)) !important;
}

.import-dialog-wrap :deep(.el-dialog__body) {
  padding: 0 !important;
  background: var(--bg-surface, var(--table-row-bg, #ffffff)) !important;
}

.import-dialog-wrap :deep(.el-dialog__footer) {
  padding: 12px 22px !important;
  border-top: 1px solid var(--border-subtle, var(--table-divider, rgba(15, 23, 42, 0.08))) !important;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc)) !important;
}

.modal-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title-text {
  font-size: calc(16.5px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
}

/* 按钮渐变规范 */
.import-dialog-wrap :deep(.el-button--primary:not(.is-link):not(.guardrail-batch-btn)) {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6) 0%, var(--theme-grad-end, #1d4ed8) 100%) !important;
  border-color: var(--theme-primary, #2563eb) !important;
  box-shadow: 0 2px 10px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.42);
}

.import-dialog-wrap :deep(.el-button--primary:not(.is-link):not(.guardrail-batch-btn):hover) {
  filter: brightness(1.08);
}

/* ================= 2. 步骤指示条 ================= */
.wizard-stepper {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 11px 22px;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border-bottom: 1px solid var(--border-subtle, var(--table-divider, rgba(15, 23, 42, 0.08)));
}

.stepper-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 9px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid transparent;
  transition: all 0.18s ease;
  user-select: none;
}

.stepper-item.is-clickable {
  cursor: pointer;
}

.stepper-item.is-clickable:hover {
  border-color: rgba(var(--theme-rgb, 37, 99, 235), 0.36);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.08);
}

.stepper-item.is-active {
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.11);
  border-color: rgba(var(--theme-rgb, 37, 99, 235), 0.34);
}

.stepper-item.is-done {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.24);
}

.stepper-index {
  width: calc(26px * var(--app-font-scale, 1));
  height: calc(26px * var(--app-font-scale, 1));
  border-radius: 50%;
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(148, 163, 184, 0.18);
  color: var(--text-sub, #64748b);
  flex-shrink: 0;
}

.stepper-item.is-active .stepper-index {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6), var(--theme-grad-end, #1d4ed8));
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(var(--theme-rgb, 37, 99, 235), 0.35);
}

.stepper-item.is-done .stepper-index {
  background: #10b981;
  color: #ffffff;
}

.stepper-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stepper-title {
  font-size: calc(14px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
  line-height: 1.25;
  white-space: nowrap;
}

/* ================= 3. 步骤体通用布局 ================= */
.step-main-body {
  padding: 18px 22px;
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 11px;
}

.section-label {
  font-size: calc(13.5px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.section-label::before {
  content: '';
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: var(--theme-primary, #1677ff);
  display: inline-block;
}

/* ================= 4. 四大渠道卡片 ================= */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.channel-card {
  border: 1.5px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  border-radius: 12px;
  padding: 13px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.channel-card:hover {
  border-color: var(--channel-accent, var(--theme-primary, #1677ff));
  transform: translateY(-2px);
}

.channel-card.is-selected {
  border-color: var(--channel-accent, var(--theme-primary, #1677ff));
  background: var(--channel-tint, rgba(var(--theme-rgb, 37, 99, 235), 0.08));
  box-shadow: 0 0 0 1px var(--channel-accent, var(--theme-primary, #1677ff)), 0 8px 20px -6px var(--channel-glow, rgba(var(--theme-rgb, 37, 99, 235), 0.25));
}

.channel-card.channel-wechat { --channel-accent: #07c160; --channel-tint: rgba(7, 193, 96, 0.08); --channel-glow: rgba(7, 193, 96, 0.24); }
.channel-card.channel-alipay { --channel-accent: #1677ff; --channel-tint: rgba(22, 119, 255, 0.08); --channel-glow: rgba(22, 119, 255, 0.24); }
.channel-card.channel-bank { --channel-accent: #6366f1; --channel-tint: rgba(99, 102, 241, 0.08); --channel-glow: rgba(99, 102, 241, 0.24); }
.channel-card.channel-paypal { --channel-accent: #0070ba; --channel-tint: rgba(0, 112, 186, 0.08); --channel-glow: rgba(0, 112, 186, 0.24); }

.channel-check-badge {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--channel-accent, var(--theme-primary, #1677ff));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.6);
  transition: all 0.18s ease;
}

.channel-card.is-selected .channel-check-badge {
  opacity: 1;
  transform: scale(1);
}

.channel-icon-emblem {
  width: 44px;
  height: 44px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.channel-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.channel-name {
  font-size: calc(14.5px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
  line-height: 1.2;
  white-space: nowrap;
}

.channel-tag {
  font-size: calc(12px * var(--app-font-scale, 1));
  font-weight: 600;
  padding: 1px calc(7px * var(--app-font-scale, 1));
  border-radius: 4px;
  line-height: 1.35;
  white-space: nowrap;
  width: fit-content;
}

.tag-wechat { background: rgba(7, 193, 96, 0.14); color: #34d399; }
.tag-alipay { background: rgba(22, 119, 255, 0.14); color: #60a5fa; }
.tag-bank { background: rgba(99, 102, 241, 0.14); color: #818cf8; }
.tag-paypal { background: rgba(0, 112, 186, 0.14); color: #38bdf8; }

html:not(.dark) .tag-wechat { color: #059669; }
html:not(.dark) .tag-alipay { color: #1677ff; }
html:not(.dark) .tag-bank { color: #4f46e5; }
html:not(.dark) .tag-paypal { color: #0070ba; }

/* ================= 5. 拖拽文件卡片 ================= */
.upload-zone-card {
  border: 1.5px dashed var(--border-strong, var(--table-border-color, rgba(148, 163, 184, 0.2)));
  border-radius: 12px;
  padding: calc(18px * var(--app-font-scale, 1)) calc(20px * var(--app-font-scale, 1));
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.03);
  transition: all 0.18s ease;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.upload-zone-card:hover,
.upload-zone-card.is-dragover {
  border-color: var(--theme-primary, #1677ff);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.08);
}

.upload-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.upload-icon-circle {
  width: calc(46px * var(--app-font-scale, 1));
  height: calc(46px * var(--app-font-scale, 1));
  border-radius: 11px;
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.14);
  color: var(--theme-light, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: calc(22px * var(--app-font-scale, 1));
  flex-shrink: 0;
}

html:not(.dark) .upload-icon-circle {
  color: var(--theme-primary, #1677ff);
}

.upload-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.upload-filename {
  font-size: calc(15px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upload-sub {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mapped-fields-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.field-chip {
  font-size: calc(12px * var(--app-font-scale, 1));
  padding: 2px calc(8px * var(--app-font-scale, 1));
  border-radius: 5px;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  color: var(--text-regular, #334155);
  font-weight: 500;
}

/* ================= 6. 弹窗内表格与排版 ================= */
.modal-table-shell {
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  border-radius: 10px;
  overflow: hidden;
}

.custom-bill-table {
  width: 100%;
  --el-table-border-color: var(--table-divider, rgba(15, 23, 42, 0.06));
  --el-table-bg-color: var(--table-row-bg, #ffffff);
  --el-table-tr-bg-color: var(--table-row-bg, #ffffff);
  --el-table-header-bg-color: var(--table-head-bg, #f8fafc);
  --el-table-row-hover-bg-color: var(--table-row-hover, #f1f6ff);
  background-color: transparent !important;
}

.custom-bill-table::before,
.custom-bill-table :deep(.el-table__inner-wrapper::before) {
  display: none !important;
}

.custom-bill-table :deep(th.el-table__cell) {
  background: var(--table-head-bg, #f8fafc) !important;
  color: var(--text-sub, #64748b) !important;
  font-weight: 600 !important;
  font-size: calc(13px * var(--app-font-scale, 1)) !important;
  padding: calc(9px * var(--app-font-scale, 1)) 0 !important;
  border-bottom: 1px solid var(--border-subtle, rgba(15, 23, 42, 0.08)) !important;
  letter-spacing: 0.2px;
}

.custom-bill-table :deep(td.el-table__cell) {
  background: transparent !important;
  color: var(--text-regular, #334155);
  font-size: calc(13px * var(--app-font-scale, 1)) !important;
  padding: calc(9px * var(--app-font-scale, 1)) 0 !important;
  border-bottom: 1px solid var(--table-divider, rgba(15, 23, 42, 0.06)) !important;
}

.custom-bill-table :deep(.el-table__row:not(.table-row-attention):not(.table-row-conflict):nth-child(even)) {
  background-color: var(--table-row-stripe, #fafcff) !important;
}

.custom-bill-table :deep(.el-table__row:not(.table-row-attention):not(.table-row-conflict):hover td.el-table__cell),
.custom-bill-table :deep(.el-table__row:not(.table-row-attention):not(.table-row-conflict).hover-row td.el-table__cell) {
  background-color: var(--table-row-hover, #f1f6ff) !important;
}

/* 分类胶囊筛选栏 */
.category-filter-bar {
  display: flex;
  align-items: center;
  gap: calc(6px * var(--app-font-scale, 1));
  flex-wrap: wrap;
  margin-bottom: 10px;
  padding: calc(8px * var(--app-font-scale, 1)) calc(11px * var(--app-font-scale, 1));
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border-radius: 10px;
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
}

.category-capsule-btn {
  font-size: calc(12.5px * var(--app-font-scale, 1));
  padding: calc(4px * var(--app-font-scale, 1)) calc(10px * var(--app-font-scale, 1));
  border-radius: 7px;
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  background: var(--bg-surface, var(--table-row-bg, #ffffff));
  color: var(--text-regular, #334155);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: calc(5px * var(--app-font-scale, 1));
  font-weight: 500;
  transition: all 0.15s ease;
  line-height: 1.35;
}

.category-capsule-btn:hover {
  border-color: var(--theme-primary, #1677ff);
  color: var(--text-main, #0f172a);
}

.category-capsule-btn.is-active {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6), var(--theme-grad-end, #1d4ed8));
  border-color: transparent;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 8px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.45);
}

.category-capsule-btn.is-warning {
  border-color: rgba(245, 158, 11, 0.42);
  background: rgba(245, 158, 11, 0.11);
  color: #fbbf24;
  font-weight: 600;
}

html:not(.dark) .category-capsule-btn.is-warning {
  color: #d97706;
}

.category-capsule-btn.is-warning .warning-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  display: inline-block;
}

.category-capsule-btn.is-warning.is-active {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border-color: transparent;
  color: #ffffff;
  box-shadow: 0 2px 8px -2px rgba(245, 158, 11, 0.48);
}

.category-capsule-btn.is-warning.is-active .warning-dot {
  background: #ffffff;
}

/* 零抖动固定高度上下文操作条 */
.table-context-dock {
  min-height: 44px;
  padding: 6px 12px;
  border-radius: 9px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

.dock-instruction-text {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  line-height: 1.4;
}

.dock-selected-text {
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 600;
  color: var(--text-main, #0f172a);
}

.table-context-dock.is-batch-active {
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.1);
  border-color: rgba(var(--theme-rgb, 37, 99, 235), 0.34);
}

/* 行内分类选择器（零双边框与未确认高亮） */
.cat-inline-select :deep(.el-select__wrapper) {
  background-color: var(--bg-input, var(--el-fill-color-blank, #f8fafc)) !important;
  box-shadow: 0 0 0 1px var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08))) inset !important;
  border-radius: 6px !important;
  min-height: 26px !important;
  padding: 2px 8px !important;
  transition: all 0.15s ease;
}

.cat-inline-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(var(--theme-rgb, 37, 99, 235), 0.5) inset !important;
}

.cat-inline-select.is-uncertain :deep(.el-select__wrapper) {
  background-color: rgba(245, 158, 11, 0.12) !important;
  box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.48) inset !important;
}

.cat-inline-select.is-uncertain :deep(.el-select__selected-item) {
  color: #fbbf24 !important;
  font-weight: 700 !important;
}

html:not(.dark) .cat-inline-select.is-uncertain :deep(.el-select__selected-item) {
  color: #d97706 !important;
}

/* 步骤 2 常驻内嵌警示横幅 */
.guardrail-inline-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 42px;
  padding: 8px 14px;
  margin-bottom: 10px;
  border-radius: 9px;
  background: rgba(245, 158, 11, 0.09);
  border: 1px solid rgba(245, 158, 11, 0.38);
  box-shadow: 0 4px 16px -4px rgba(245, 158, 11, 0.16);
  transition: all 0.22s ease;
}

html:not(.dark) .guardrail-inline-banner {
  background: #fffbeb;
  border-color: #fcd34d;
  box-shadow: 0 4px 14px -4px rgba(217, 119, 6, 0.12);
}

.guardrail-inline-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.guardrail-icon-box {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: rgba(245, 158, 11, 0.16);
  color: #f59e0b;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.guardrail-inline-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.guardrail-inline-title {
  font-size: calc(13.5px * var(--app-font-scale, 1));
  font-weight: 700;
  color: #f59e0b;
  line-height: 1.35;
}

html:not(.dark) .guardrail-inline-title {
  color: #b45309;
}

.guardrail-inline-desc {
  font-size: calc(12.5px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  line-height: 1.35;
}

html:not(.dark) .guardrail-inline-desc {
  color: #92400e;
}

.guardrail-inline-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.banner-quick-select-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.guardrail-btn {
  height: calc(28px * var(--app-font-scale, 1)) !important;
  line-height: calc(28px * var(--app-font-scale, 1)) !important;
  padding: 0 calc(13px * var(--app-font-scale, 1)) !important;
  font-size: calc(12px * var(--app-font-scale, 1)) !important;
  font-weight: 600 !important;
  border-radius: 6px !important;
  border: none !important;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
  box-sizing: border-box !important;
  transition: all 0.2s ease;
  vertical-align: middle;
}

.guardrail-btn-apply {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8) !important;
  color: #ffffff !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3) !important;
}

.guardrail-btn-apply:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.45) !important;
}

.guardrail-btn-apply:disabled {
  background: #334155 !important;
  color: #94a3b8 !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
  opacity: 0.55;
}

html:not(.dark) .guardrail-btn-apply:disabled {
  background: #e2e8f0 !important;
  color: #94a3b8 !important;
}

.guardrail-btn-primary {
  background: linear-gradient(135deg, #f59e0b, #d97706) !important;
  color: #ffffff !important;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.35) !important;
}

.guardrail-btn-primary:hover {
  opacity: 0.94;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.45) !important;
}

.guardrail-btn-primary:active {
  transform: translateY(0);
}

.guardrail-close-btn {
  background: none;
  border: none;
  color: var(--text-sub, #64748b);
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  line-height: 1;
  border-radius: 4px;
  transition: all 0.18s;
}

.guardrail-close-btn:hover {
  color: var(--text-main, #0f172a);
  background: rgba(245, 158, 11, 0.12);
}

.guardrail-slide-enter-active,
.guardrail-slide-leave-active {
  transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.guardrail-slide-enter-from,
.guardrail-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* 未分类行染色与震动动效 */
@keyframes rowShakeAnim {
  0%, 100% { transform: translateX(0); }
  15% { transform: translateX(-8px); }
  30% { transform: translateX(7px); }
  45% { transform: translateX(-5px); }
  60% { transform: translateX(4px); }
  75% { transform: translateX(-2px); }
  90% { transform: translateX(1px); }
}

:deep(.custom-bill-table .table-row-shaking td),
:deep(.custom-bill-table tr.table-row-shaking td) {
  animation: rowShakeAnim 0.75s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
  will-change: transform;
}

/* 深色模式下的实色暖金琥珀高亮底色（全单元格纯色平铺，无渐变） */
.custom-bill-table :deep(tr.table-row-attention td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-attention td.el-table__cell),
.custom-bill-table :deep(tr.table-row-conflict td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-conflict td.el-table__cell) {
  --el-table-tr-bg-color: rgba(245, 158, 11, 0.28) !important;
  --el-table-row-hover-bg-color: rgba(245, 158, 11, 0.40) !important;
  background-color: rgba(245, 158, 11, 0.28) !important;
  background: rgba(245, 158, 11, 0.28) !important;
  border-bottom: 1px solid rgba(245, 158, 11, 0.45) !important;
  box-shadow: inset 0 -1px 0 rgba(245, 158, 11, 0.4) !important;
  transition: background 0.15s ease, background-color 0.15s ease;
}

/* 专用的高亮行鼠标悬停纯色暖色（无渐变，明亮琥珀） */
.custom-bill-table :deep(tr.table-row-attention:hover td.el-table__cell),
.custom-bill-table :deep(tr.table-row-attention.hover-row td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-attention:hover td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-attention.hover-row td.el-table__cell),
.custom-bill-table :deep(tr.table-row-conflict:hover td.el-table__cell),
.custom-bill-table :deep(tr.table-row-conflict.hover-row td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-conflict:hover td.el-table__cell),
.custom-bill-table :deep(.el-table__row.table-row-conflict.hover-row td.el-table__cell) {
  background-color: rgba(245, 158, 11, 0.40) !important;
  background: rgba(245, 158, 11, 0.40) !important;
  border-bottom: 1px solid rgba(245, 158, 11, 0.65) !important;
}

.custom-bill-table :deep(tr.table-row-attention td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-attention td.el-table__cell:first-child),
.custom-bill-table :deep(tr.table-row-conflict td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-conflict td.el-table__cell:first-child) {
  border-left: 5px solid #f59e0b !important;
}

.custom-bill-table :deep(tr.table-row-attention:hover td.el-table__cell:first-child),
.custom-bill-table :deep(tr.table-row-attention.hover-row td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-attention:hover td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-attention.hover-row td.el-table__cell:first-child),
.custom-bill-table :deep(tr.table-row-conflict:hover td.el-table__cell:first-child),
.custom-bill-table :deep(tr.table-row-conflict.hover-row td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-conflict:hover td.el-table__cell:first-child),
.custom-bill-table :deep(.el-table__row.table-row-conflict.hover-row td.el-table__cell:first-child) {
  border-left: 5px solid #fbbf24 !important;
}

/* 浅色模式下的明亮琥珀高亮底色与悬停暖色 */
html:not(.dark) .custom-bill-table :deep(tr.table-row-attention td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-attention td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(tr.table-row-conflict td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-conflict td.el-table__cell) {
  --el-table-tr-bg-color: #fef3c7 !important;
  --el-table-row-hover-bg-color: #fde68a !important;
  background: #fef3c7 !important;
  border-bottom: 1px solid #fcd34d !important;
}

html:not(.dark) .custom-bill-table :deep(tr.table-row-attention:hover td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(tr.table-row-attention.hover-row td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-attention:hover td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-attention.hover-row td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(tr.table-row-conflict:hover td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(tr.table-row-conflict.hover-row td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-conflict:hover td.el-table__cell),
html:not(.dark) .custom-bill-table :deep(.el-table__row.table-row-conflict.hover-row td.el-table__cell) {
  background: #fde68a !important;
}

/* ================= 7. 步骤 3 预检与排重专属元素 ================= */
.precheck-stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1.35fr;
  gap: calc(10px * var(--app-font-scale, 1));
  margin-bottom: 12px;
}

.precheck-stat-card {
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  border-radius: 10px;
  padding: calc(8px * var(--app-font-scale, 1)) calc(12px * var(--app-font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: calc(8px * var(--app-font-scale, 1));
  min-width: 0;
  transition: all 0.16s ease;
}

.stat-label {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.stat-val {
  font-size: calc(19px * var(--app-font-scale, 1));
  font-weight: 800;
  white-space: nowrap;
}

.stat-val.is-amount {
  font-size: calc(15px * var(--app-font-scale, 1));
  white-space: nowrap;
}

.stat-badge-success {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: calc(3px * var(--app-font-scale, 1)) calc(8px * var(--app-font-scale, 1));
  border-radius: 6px;
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 600;
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.28);
  white-space: nowrap;
  flex-shrink: 0;
}

.dedup-banner {
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.11) 0%, rgba(245, 158, 11, 0.03) 100%);
  border: 1px solid rgba(245, 158, 11, 0.32);
  border-radius: 9px;
  padding: calc(8px * var(--app-font-scale, 1)) calc(13px * var(--app-font-scale, 1));
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.dedup-banner :deep(.el-radio-button__inner) {
  font-size: calc(13px * var(--app-font-scale, 1)) !important;
  padding: calc(6px * var(--app-font-scale, 1)) calc(14px * var(--app-font-scale, 1)) !important;
  height: auto !important;
  line-height: 1.3 !important;
}

.dedup-banner-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: calc(13.5px * var(--app-font-scale, 1));
  color: var(--text-main, #0f172a);
}

.dedup-dot {
  width: calc(22px * var(--app-font-scale, 1));
  height: calc(22px * var(--app-font-scale, 1));
  border-radius: 6px;
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cat-readonly-cell {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
}

.cat-readonly-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 500;
  background: rgba(99, 102, 241, 0.08);
  color: var(--theme-light, #818cf8);
  border: 1px solid rgba(99, 102, 241, 0.18);
  line-height: 1.2;
}

html:not(.dark) .cat-readonly-tag {
  background: rgba(79, 70, 229, 0.06);
  color: #4f46e5;
  border-color: rgba(79, 70, 229, 0.18);
}

.custom-table-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: calc(12.5px * var(--app-font-scale, 1));
  line-height: 1.4;
  border-radius: 5px;
  font-weight: 600;
  border: 1px solid transparent;
  white-space: nowrap;
}

.custom-table-tag.tag-status-normal { background: rgba(16, 185, 129, 0.14); color: #34d399; border-color: rgba(16, 185, 129, 0.28); }
.custom-table-tag.tag-status-conflict-skip { background: rgba(245, 158, 11, 0.14); color: #fbbf24; border-color: rgba(245, 158, 11, 0.28); }
.custom-table-tag.tag-status-conflict-keep { background: rgba(239, 68, 68, 0.14); color: #f87171; border-color: rgba(239, 68, 68, 0.28); }
.custom-table-tag.tag-status-foreign { background: rgba(59, 130, 246, 0.14); color: #60a5fa; border-color: rgba(59, 130, 246, 0.28); }

html:not(.dark) .custom-table-tag.tag-status-normal { color: #059669; }
html:not(.dark) .custom-table-tag.tag-status-conflict-skip { color: #d97706; }
html:not(.dark) .custom-table-tag.tag-status-conflict-keep { color: #dc2626; }
html:not(.dark) .custom-table-tag.tag-status-foreign { color: #2563eb; }

/* ================= 8. 步骤 4 完成页专属元素 ================= */
.finish-wrap {
  text-align: center;
  padding: 24px 16px 12px;
}

.finish-icon-circle {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  font-size: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.finish-title {
  font-size: calc(20px * var(--app-font-scale, 1));
  font-weight: 700;
  color: var(--text-main, #0f172a);
  margin-bottom: 6px;
}

.finish-desc {
  font-size: calc(14px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  margin: 0 auto 20px;
}

.report-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-width: 680px;
  margin: 0 auto 8px;
}

.report-card-item {
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  border-radius: 10px;
  padding: 14px 12px;
  text-align: center;
}

/* ================= 9. 单元格通用微观元素 ================= */
.cell-merchant-product {
  display: flex;
  align-items: center;
  gap: calc(7px * var(--app-font-scale, 1));
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
}

.cell-merchant-product .cell-title {
  font-weight: 600;
  color: var(--text-main, #0f172a);
  font-size: calc(14px * var(--app-font-scale, 1));
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  max-width: 100%;
}

.cell-merchant-product .cell-sub {
  font-size: calc(12.8px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.cell-merchant-product.is-double-line {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  white-space: normal;
}

.cleaned-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px calc(5px * var(--app-font-scale, 1));
  font-size: calc(11px * var(--app-font-scale, 1));
  font-weight: 600;
  border-radius: 4px;
  color: var(--theme-light, #60a5fa);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.14);
  border: 1px solid rgba(var(--theme-rgb, 37, 99, 235), 0.28);
  margin-left: 6px;
  cursor: help;
  line-height: 1.25;
  white-space: nowrap !important;
  flex-shrink: 0 !important;
}

html:not(.dark) .cleaned-tag {
  color: var(--theme-primary, #2563eb);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.09);
}

.payment-cell-badge {
  display: inline-flex;
  align-items: center;
  gap: calc(5px * var(--app-font-scale, 1));
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-regular, #334155);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.payment-mini-icon {
  width: calc(20px * var(--app-font-scale, 1));
  height: calc(20px * var(--app-font-scale, 1));
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.amount-main {
  font-weight: 700;
  font-size: calc(14.5px * var(--app-font-scale, 1));
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.amount-expense {
  color: #f87171;
}

.amount-income {
  color: #34d399;
}

html:not(.dark) .amount-expense {
  color: #e11d48;
}

html:not(.dark) .amount-income {
  color: #059669;
}

.amount-sub {
  font-size: calc(11.5px * var(--app-font-scale, 1));
  color: var(--text-muted, #94a3b8);
  margin-top: 1px;
}

.table-cell-time {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
}

.precheck-stat-card.is-actionable {
  cursor: pointer;
}
.precheck-stat-card.is-actionable:hover {
  border-color: rgba(245, 158, 11, 0.55);
  background: rgba(245, 158, 11, 0.08);
}

.cell-merchant-product .cell-divider {
  color: var(--text-muted, #94a3b8);
  opacity: 0.45;
  font-size: calc(12px * var(--app-font-scale, 1));
  user-select: none;
  flex-shrink: 0;
}
.cell-merchant-product.is-double-line .cell-divider {
  display: none;
}
.cell-merchant-product.is-double-line .cell-sub {
  font-size: calc(12px * var(--app-font-scale, 1));
  line-height: 1.35;
  white-space: nowrap;
  max-width: 100%;
}

/* 冲突比对卡片 */
.conflict-compare-card {
  margin-bottom: 12px;
  padding: calc(10px * var(--app-font-scale, 1)) calc(14px * var(--app-font-scale, 1));
  border-radius: 9px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px dashed rgba(245, 158, 11, 0.35);
}
.conflict-compare-header {
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 600;
  color: #f59e0b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.conflict-policy-hint {
  font-size: calc(12px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  font-weight: normal;
}
.conflict-compare-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 120px;
  overflow-y: auto;
}
.conflict-compare-item {
  font-size: calc(13px * var(--app-font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-surface, var(--table-row-bg, rgba(0, 0, 0, 0.2)));
  padding: calc(6px * var(--app-font-scale, 1)) calc(10px * var(--app-font-scale, 1));
  border-radius: 6px;
}
.conflict-info-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}
.conflict-time {
  color: var(--text-sub, #64748b);
  font-size: calc(12px * var(--app-font-scale, 1));
}
.conflict-payee {
  color: var(--text-main, #0f172a);
  font-size: calc(13.5px * var(--app-font-scale, 1));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conflict-detail {
  color: var(--text-sub, #64748b);
  font-size: calc(12.5px * var(--app-font-scale, 1));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conflict-info-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.conflict-amount {
  font-weight: 700;
  font-size: calc(14.5px * var(--app-font-scale, 1));
}
.conflict-reason-badge {
  font-size: calc(11.5px * var(--app-font-scale, 1));
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
html:not(.dark) .conflict-reason-badge {
  color: #d97706;
}

/* 渐变按钮体系 */
.btn-action {
  border-radius: 8px !important;
  font-weight: 600 !important;
  font-size: calc(13.5px * var(--app-font-scale, 1)) !important;
  padding: calc(8px * var(--app-font-scale, 1)) calc(14px * var(--app-font-scale, 1)) !important;
  height: calc(36px * var(--app-font-scale, 1)) !important;
  border: 1px solid transparent !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}
.btn-action:hover {
  transform: translateY(-1px);
}

.btn-toolbar-search {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6) 0%, var(--theme-grad-end, #1d4ed8) 100%) !important;
  border-color: rgba(255, 255, 255, 0.14) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 12px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.22);
}
.btn-toolbar-search:hover {
  filter: brightness(1.08);
  box-shadow: 0 6px 18px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.58), inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.btn-toolbar-reset {
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc)) !important;
  border-color: var(--border-strong, var(--table-border-color, rgba(148, 163, 184, 0.2))) !important;
  color: var(--text-regular, #64748b) !important;
}
.btn-toolbar-reset:hover {
  color: var(--text-main, #0f172a) !important;
  border-color: rgba(var(--theme-rgb, 37, 99, 235), 0.4) !important;
}

.btn-toolbar-create {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 10px -2px rgba(16, 185, 129, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.18);
}
.btn-toolbar-create:hover {
  filter: brightness(1.07);
}
</style>
