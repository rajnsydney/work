package com.bt.nextgen.api.account.v2.service.performance;

import com.bt.nextgen.api.account.v2.model.DateValueDto;
import com.bt.nextgen.api.account.v2.model.performance.AccountBenchmarkPerformanceDto;
import com.bt.nextgen.api.account.v2.model.performance.AccountBenchmarkPerformanceKey;
import com.bt.nextgen.api.account.v2.util.PerformanceReportUtil;
import com.btfin.panorama.core.security.encryption.EncodedString;
import com.bt.nextgen.service.ServiceErrors;
import com.bt.nextgen.service.avaloq.portfolio.performance.WrapAccountPerformanceImpl;
import com.bt.nextgen.service.integration.account.AccountKey;
import com.bt.nextgen.service.integration.performance.Performance;
import com.bt.nextgen.service.integration.performance.PerformancePeriodType;
import com.bt.nextgen.service.integration.portfolio.performance.AccountPerformanceIntegrationService;
import com.bt.nextgen.service.integration.portfolio.performance.WrapAccountPerformance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@Service
public class AccountBenchmarkPerformanceChartDtoServiceImpl implements AccountBenchmarkPerformanceChartDtoService {

    @Autowired
    private AccountPerformanceIntegrationService accountService;

    @Override
    public AccountBenchmarkPerformanceDto find(AccountBenchmarkPerformanceKey key, ServiceErrors serviceErrors) {
        String accountId = EncodedString.toPlainText(key.getAccountId());

        WrapAccountPerformanceImpl perfSummary = (WrapAccountPerformanceImpl) accountService.loadAccountPerformanceReport(
                AccountKey.valueOf(accountId), key.getBenchmarkId(), key.getStartDate(), key.getEndDate(), serviceErrors);

        return buildAccountBenchmarkPerformanceDto(key, perfSummary);
    }

    private AccountBenchmarkPerformanceDto buildAccountBenchmarkPerformanceDto(AccountBenchmarkPerformanceKey key,
            WrapAccountPerformance accountPerformance) {
        PerformancePeriodType detailedPeriodType = PerformanceReportUtil
                .getAccountPerformancePeriodForLineGraph(accountPerformance);
        List<Performance> chartPerformanceData = PerformanceReportUtil.getPerformanceData(detailedPeriodType, accountPerformance);
        List<DateValueDto> benchmarkChartData = new ArrayList<>();
        for (Performance data : chartPerformanceData) {
            BigDecimal bmrkPerformance = data.getBmrkRor() != null ? data.getBmrkRor().divide(BigDecimal.valueOf(100)) : null;
            benchmarkChartData.add(new DateValueDto(data.getPeriodSop(), bmrkPerformance));
        }
        return new AccountBenchmarkPerformanceDto(key, benchmarkChartData, detailedPeriodType);
    }

}
