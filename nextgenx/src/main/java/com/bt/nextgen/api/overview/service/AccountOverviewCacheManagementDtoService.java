package com.bt.nextgen.api.overview.service;


import com.bt.nextgen.api.account.v2.model.AccountKey;
import com.bt.nextgen.api.overview.model.AccountOverviewDetailsDto;
import com.bt.nextgen.api.overview.model.CacheRefreshDto;
import com.bt.nextgen.core.api.dto.FindAllDtoService;
import com.bt.nextgen.core.api.dto.FindByKeyDtoService;
import com.bt.nextgen.core.api.dto.FindOneDtoService;
import com.bt.nextgen.core.api.operation.FindOne;

/**
 *
 */
public interface AccountOverviewCacheManagementDtoService extends FindByKeyDtoService<AccountKey, CacheRefreshDto>
{

}