package com.bt.nextgen.api.product.service;

import com.bt.nextgen.api.adviser.model.AdviserSearchDto;
import com.bt.nextgen.api.adviser.service.AdviserSearchDtoService;
import com.bt.nextgen.api.product.model.ProductDto;
import com.bt.nextgen.api.product.model.ProductKey;
import com.bt.nextgen.core.api.ApiVersion;
import com.bt.nextgen.core.api.exception.NotAllowedException;
import com.bt.nextgen.core.api.operation.ApiSearchCriteria;
import com.btfin.panorama.core.security.profile.UserProfileService;
import com.btfin.panorama.core.security.encryption.EncodedString;
import com.bt.nextgen.core.util.LambdaMatcher;
import com.btfin.panorama.service.exception.FailFastErrorsImpl;
import com.bt.nextgen.service.ServiceErrors;
import com.btfin.panorama.service.integration.broker.Broker;
import com.bt.nextgen.service.integration.broker.BrokerIntegrationService;
import com.bt.nextgen.service.integration.broker.BrokerKey;
import com.bt.nextgen.service.integration.product.Product;
import com.bt.nextgen.service.integration.product.ProductIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.selectFirst;

@Deprecated
@Service
public class ProductDtoServiceImpl implements ProductDtoService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDtoServiceImpl.class);

	@Autowired
	private ProductIntegrationService productIntegrationService;

    @Autowired
    private BrokerIntegrationService brokerIntegrationService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ProductDtoConverter productDtoConverter;

    @Autowired
    private AdviserSearchDtoService adviserSearchDtoService;

	@Override
	public ProductDto find(ProductKey key, ServiceErrors serviceErrors)
	{
		String strProdId = new EncodedString(key.getProductId()).plainText();
        com.bt.nextgen.service.integration.product.ProductKey productKey =
                com.bt.nextgen.service.integration.product.ProductKey.valueOf(strProdId);
        return productDtoConverter.convert(productIntegrationService.getProductDetail(productKey, serviceErrors));
	}

	@Override
	public List<ProductDto> findAll(ServiceErrors errors)
	{
		//TODO - UPS REFACTOR1 - We need to pass in more information from the JSON API about the context of the dealergroup (whether it is adviser or account specific).
        Broker broker = userProfileService.getDealerGroupBroker();
        BrokerKey brokerKey = broker.getKey();
        return dealerGroupProducts(brokerKey, errors);
	}

    private boolean canViewProductsForAdviser(final String adviserPositionId){
        List<ApiSearchCriteria> apiSearchCriterias = new ArrayList<>();
        List<AdviserSearchDto> advisersList = adviserSearchDtoService.search(apiSearchCriterias, new FailFastErrorsImpl());
        AdviserSearchDto matchedAdviser = selectFirst(advisersList, new LambdaMatcher<AdviserSearchDto>() {
            @Override
            protected boolean matchesSafely(AdviserSearchDto adviser) {
                return EncodedString.toPlainText(adviser.getAdviserPositionId()).equals(adviserPositionId);
            }
        });
        return matchedAdviser != null;
    }

    private void checkAdviserIdIsAllowedForLoggedInUser(String adviserId){
        if(!canViewProductsForAdviser(adviserId)) {
            throw new NotAllowedException(ApiVersion.CURRENT_VERSION, "The user cannot view products for this adviser");
        }
    }

    @Override
    public List<ProductDto> search(List<ApiSearchCriteria> criteriaList, ServiceErrors errors) {
        final ApiSearchCriteria criteria = criteriaList.get(0);
        final String decodedValue = EncodedString.toPlainText(criteria.getValue());
        final BrokerKey dealerGroupKey;
        switch (criteria.getProperty()) {
            case "positionId":
                checkAdviserIdIsAllowedForLoggedInUser(decodedValue);
                Broker broker = brokerIntegrationService.getBroker(BrokerKey.valueOf(decodedValue), errors);
                dealerGroupKey = broker.getDealerKey();
                break;
            case "dealerGroupId":
                //TODO: this is not checked for access by users who should not see data for this dealer group; please fix later
				dealerGroupKey = BrokerKey.valueOf(decodedValue);
                break;
			default:
				throw new IllegalArgumentException("Unknown property "+criteria.getProperty());
		}
        return dealerGroupProducts(dealerGroupKey, errors);
    }

    private List<ProductDto> dealerGroupProducts(BrokerKey dealerGroupKey, ServiceErrors errors) {
        final List<Product> products = productIntegrationService.getDealerGroupProductList(dealerGroupKey, errors);
        LOGGER.debug("Retrieved {} products for Dealer Group ID: {}", products.size(), dealerGroupKey.getId());
        if (LOGGER.isTraceEnabled()) {
            for (Product product : products) {
                LOGGER.trace("\tProduct[{}]: {}", product.getProductKey().getId(), product.getProductName());
            }
        }
        return convert(products, productDtoConverter);
    }
}
