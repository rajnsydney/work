package com.bt.nextgen.api.trading.v1.service.termdeposittradeassetservice;

import com.bt.nextgen.api.asset.model.AssetDto;
import com.bt.nextgen.api.trading.v1.model.TermDepositTradeAssetDto;
import com.bt.nextgen.api.trading.v1.model.TradeAssetDto;
import com.bt.nextgen.api.trading.v1.service.assetbuilder.TermDepositAssetBuilder;
import com.bt.nextgen.api.trading.v1.util.TradableAssetsDtoServiceFilter;
import com.bt.nextgen.api.trading.v1.util.TradableAssetsDtoServiceHelper;
import com.bt.nextgen.service.ServiceErrors;
import com.bt.nextgen.service.integration.account.AccountKey;
import com.bt.nextgen.service.integration.account.DistributionMethod;
import com.btfin.panorama.service.integration.account.WrapAccountDetail;
import com.bt.nextgen.service.integration.asset.Asset;
import com.bt.nextgen.service.integration.asset.AssetIntegrationService;
import com.bt.nextgen.service.integration.asset.AssetStatus;
import com.bt.nextgen.service.integration.asset.TermDepositAssetDetail;
import com.bt.nextgen.service.integration.bankdate.BankDateIntegrationService;
import com.bt.nextgen.service.integration.broker.BrokerKey;
import com.btfin.panorama.service.exception.FailFastErrorsImpl;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TermDepositTradeAssetServiceTest {

    @InjectMocks
    private TermDepositTradeAssetService termDepositTradeAssetService;

    @Mock
    private AssetIntegrationService assetIntegrationService;

    @Mock
    private TradableAssetsDtoServiceHelper tradableAssetsDtoServiceHelper;

    @Mock
    private TradableAssetsDtoServiceFilter tradableAssetsDtoServiceFilter;

    @Mock
    private TermDepositAssetBuilder termDepositAssetBuilder;

    @Mock
    private BankDateIntegrationService bankDateService;

    private Asset openAsset1;
    private Asset openAsset2;
    private Asset openAsset3;
    private Asset filteredTdAsset;
    private List<Asset> availableAssets;
    private Map<String, Asset> filteredAvailableAssets;
    private Map<String, Asset> filteredTdAssets;

    @Before
    public void setUp() throws Exception {
        Asset suspendedAsset = Mockito.mock(Asset.class);
        Mockito.when(suspendedAsset.getAssetId()).thenReturn("suspendedAsset");
        Mockito.when(suspendedAsset.getStatus()).thenReturn(AssetStatus.SUSPENDED);

        Asset terminatedAsset = Mockito.mock(Asset.class);
        Mockito.when(terminatedAsset.getAssetId()).thenReturn("terminatedAsset");
        Mockito.when(terminatedAsset.getStatus()).thenReturn(AssetStatus.TERMINATED);

        Asset closedToNewAsset = Mockito.mock(Asset.class);
        Mockito.when(closedToNewAsset.getAssetId()).thenReturn("closedToNewAsset");
        Mockito.when(closedToNewAsset.getStatus()).thenReturn(AssetStatus.CLOSED_TO_NEW);

        Asset closedAsset = Mockito.mock(Asset.class);
        Mockito.when(closedAsset.getAssetId()).thenReturn("closedAsset");
        Mockito.when(closedAsset.getStatus()).thenReturn(AssetStatus.CLOSED);

        openAsset1 = Mockito.mock(Asset.class);
        Mockito.when(openAsset1.getAssetId()).thenReturn("openAsset1");
        Mockito.when(openAsset1.getStatus()).thenReturn(AssetStatus.OPEN);

        openAsset2 = Mockito.mock(Asset.class);
        Mockito.when(openAsset2.getAssetId()).thenReturn("openAsset2");
        Mockito.when(openAsset2.getStatus()).thenReturn(AssetStatus.OPEN);

        openAsset3 = Mockito.mock(Asset.class);
        Mockito.when(openAsset3.getAssetId()).thenReturn("openAsset3");
        Mockito.when(openAsset3.getStatus()).thenReturn(AssetStatus.OPEN);

        filteredTdAsset = Mockito.mock(Asset.class);
        Mockito.when(filteredTdAsset.getAssetId()).thenReturn("filteredTdAssetId");
        Mockito.when(filteredTdAsset.getStatus()).thenReturn(AssetStatus.OPEN);

        availableAssets = new ArrayList<Asset>();
        availableAssets.add(suspendedAsset);
        availableAssets.add(terminatedAsset);
        availableAssets.add(closedToNewAsset);
        availableAssets.add(closedAsset);
        availableAssets.add(openAsset1);
        availableAssets.add(openAsset2);
        availableAssets.add(openAsset3);

        filteredAvailableAssets = new HashMap<String, Asset>();
        filteredAvailableAssets.put(suspendedAsset.getAssetId(), suspendedAsset);
        filteredAvailableAssets.put(terminatedAsset.getAssetId(), terminatedAsset);
        filteredAvailableAssets.put(closedToNewAsset.getAssetId(), closedToNewAsset);
        filteredAvailableAssets.put(closedAsset.getAssetId(), closedAsset);
        filteredAvailableAssets.put(openAsset2.getAssetId(), openAsset2);

        filteredTdAssets = new HashMap<String, Asset>();
        filteredTdAssets.put(openAsset3.getAssetId(), openAsset3);

        TermDepositAssetDetail tdAssetDetail2 = Mockito.mock(TermDepositAssetDetail.class);
        Mockito.when(tdAssetDetail2.getAssetId()).thenReturn("openAsset2");

        TermDepositAssetDetail tdAssetDetail3 = Mockito.mock(TermDepositAssetDetail.class);
        Mockito.when(tdAssetDetail3.getAssetId()).thenReturn("openAsset3");

        TermDepositAssetDetail filteredTdAssetDetail = Mockito.mock(TermDepositAssetDetail.class);
        Mockito.when(filteredTdAssetDetail.getAssetId()).thenReturn("filteredTdAssetId");

        Map<String, TermDepositAssetDetail> termDepositAssetDetails = new HashMap<>();
        termDepositAssetDetails.put(filteredTdAssetDetail.getAssetId(), filteredTdAssetDetail);
        termDepositAssetDetails.put(tdAssetDetail3.getAssetId(), tdAssetDetail3);

        Map<String, Asset> termDepositAssets = new HashMap<>();
        termDepositAssets.put(filteredTdAsset.getAssetId(), filteredTdAsset);
        termDepositAssets.put(openAsset3.getAssetId(), openAsset3);

        Map<String, TermDepositAssetDetail> filteredTermDepositAssetDetails = new HashMap<>();
        filteredTermDepositAssetDetails.put(tdAssetDetail2.getAssetId(), tdAssetDetail2);

        WrapAccountDetail mockedWrapAccountDetail = Mockito.mock(WrapAccountDetail.class);
        Mockito.when(mockedWrapAccountDetail.getAccountKey()).thenReturn(AccountKey.valueOf("accountId"));

        Mockito.when(assetIntegrationService.loadTermDepositRatesForCriteria(Mockito.any(BrokerKey.class),
                Mockito.any(String.class), Mockito.any(DateTime.class), Mockito.any(ServiceErrors.class)))
                .thenReturn(termDepositAssetDetails);

        Mockito.when(assetIntegrationService.loadAssets(Mockito.anyListOf(String.class), Mockito.any(ServiceErrors.class)))
                .thenReturn(termDepositAssets);

        Mockito.when(assetIntegrationService.loadTermDepositRates(Mockito.any(BrokerKey.class), Mockito.any(DateTime.class),
                Mockito.anyList(), Mockito.any(ServiceErrors.class))).thenReturn(filteredTermDepositAssetDetails);

        Mockito.when(tradableAssetsDtoServiceHelper.loadDistributionMethods(
                Mockito.anyCollection())).thenReturn(new HashMap<String, List<DistributionMethod>>());

        Mockito.when(tradableAssetsDtoServiceFilter.filterAvailableAssetsList(Mockito.anyListOf(Asset.class),
                Mockito.anyMapOf(String.class, Asset.class))).// thenReturn(filteredTdAssets);
                thenAnswer(new Answer<Map<String, Asset>>() {
                    @Override
                    public Map<String, Asset> answer(InvocationOnMock invocation) throws Throwable {
                        List<Asset> availableAssets = (List<Asset>) invocation.getArguments()[0];
                        Map<String, Asset> filteredAssets = (Map<String, Asset>) invocation.getArguments()[1];
                        Map<String, Asset> result = new HashMap<>();
                        for (Asset asset : availableAssets) {
                            if (filteredAssets.containsKey(asset.getAssetId())) {
                                result.put(asset.getAssetId(), asset);
                            }
                        }
                        return result;
                    }
                });

        List<TradeAssetDto> tradeAssetDtos = new ArrayList<>();
        TermDepositTradeAssetDto termDepositTradeAssetDto = Mockito.mock(TermDepositTradeAssetDto.class);
        AssetDto assetDto = Mockito.mock(AssetDto.class);
        Mockito.when(assetDto.getAssetId()).thenReturn("mockassetId");
        Mockito.when(termDepositTradeAssetDto.getAsset()).thenReturn(assetDto);
        tradeAssetDtos.add(termDepositTradeAssetDto);

        Mockito.when(termDepositAssetBuilder.buildTradeAssets(Mockito.anyMap(), Mockito.anyMap())).thenReturn(tradeAssetDtos);
        DateTime bankDate = DateTime.now();
        Mockito.when(bankDateService.getBankDate(Mockito.any(ServiceErrors.class))).thenReturn(bankDate);
    }

    @Test
    public final void testLoadTermDepositTradeAssets() {
        List<TradeAssetDto> returnedTermDepositAssetDetails = termDepositTradeAssetService.loadTermDepositTradeAssets(
                new ArrayList<String>(), "termDepositAssetId", BrokerKey.valueOf("brokerId"), availableAssets,
                filteredAvailableAssets, new FailFastErrorsImpl());
        Assert.assertEquals("mockassetId", returnedTermDepositAssetDetails.get(0).getAsset().getAssetId());
    }

    @Test
    public final void testLoadTermDepositAssetDetails() {
        Map<String, TermDepositAssetDetail> tdAssetDetails = termDepositTradeAssetService.loadTermDepositAssetDetails(null,
                "termDepositAssetId", BrokerKey.valueOf("brokerId"), availableAssets, filteredAvailableAssets,
                new FailFastErrorsImpl());
        Assert.assertEquals(3, tdAssetDetails.values().size());
        Assert.assertEquals(true, tdAssetDetails.containsKey(openAsset2.getAssetId()));
        Assert.assertEquals(true, tdAssetDetails.containsKey(openAsset3.getAssetId()));

        Assert.assertEquals(6, filteredAvailableAssets.values().size());
    }
}
