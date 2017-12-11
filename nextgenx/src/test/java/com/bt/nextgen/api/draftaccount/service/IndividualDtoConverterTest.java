package com.bt.nextgen.api.draftaccount.service;

import com.bt.nextgen.api.client.model.AddressDto;
import com.bt.nextgen.api.client.model.AddressTypeV2;
import com.bt.nextgen.api.client.model.EmailDto;
import com.bt.nextgen.api.client.model.IndividualDto;
import com.bt.nextgen.api.client.model.InvestorDto;
import com.bt.nextgen.api.client.model.PhoneDto;
import com.bt.nextgen.api.draftaccount.AbstractJsonReaderTest;
import com.bt.nextgen.api.draftaccount.model.form.ClientApplicationFormFactory;
import com.bt.nextgen.api.draftaccount.model.form.IAddressForm;
import com.bt.nextgen.api.draftaccount.model.form.IClientApplicationForm;
import com.bt.nextgen.api.draftaccount.model.form.IContactValue;
import com.bt.nextgen.api.draftaccount.model.form.IExtendedPersonDetailsForm;
import com.bt.nextgen.api.draftaccount.model.form.IIdentityVerificationForm;
import com.bt.nextgen.api.draftaccount.model.form.IPersonDetailsForm;
import com.bt.nextgen.api.draftaccount.model.form.v1.ClientApplicationFormFactoryV1;
import com.bt.nextgen.api.draftaccount.schemas.v1.OnboardingApplicationFormData;
import com.bt.nextgen.api.draftaccount.util.XMLGregorianCalendarUtil;
import com.bt.nextgen.config.ApplicationContextProvider;
import com.bt.nextgen.config.JsonObjectMapper;
import com.bt.nextgen.core.toggle.FeatureTogglesService;
import com.bt.nextgen.service.ServiceErrors;
import com.bt.nextgen.service.avaloq.code.CodeImpl;
import com.bt.nextgen.service.integration.code.StaticIntegrationService;
import com.bt.nextgen.service.integration.domain.InvestorRole;
import com.btfin.panorama.core.conversion.CodeCategory;
import com.btfin.panorama.service.exception.FailFastErrorsImpl;
import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndividualDtoConverterTest extends AbstractJsonReaderTest {

    @Mock
    private ApplicationContext appContext; //this is the context generated by SpringJUnit4ClassRunner

    @Mock
    private StaticIntegrationService staticService;

    @Mock
    private AddressDtoConverter addressDtoConverter;

    @Mock
    private ClientApplicationDetailsDtoHelperService clientApplicationDetailsDtoHelperService;

    @Mock
    private CRSTaxDetailHelperServiceImpl crsTaxDetailHelperService;


    private IExtendedPersonDetailsForm extendedPersonDetailsForm;

    private IExtendedPersonDetailsForm firstAdditionalShareholder;

    private IExtendedPersonDetailsForm personDetailFormWithOtherPhone;

    private List<IExtendedPersonDetailsForm> additionalShareholders;

    private List<IExtendedPersonDetailsForm> personFormWithTrustees;

    private List<IExtendedPersonDetailsForm> additionalMembers;

    private List<IExtendedPersonDetailsForm> TrustFormWithTrustees;

    private List<IExtendedPersonDetailsForm> additionalBeneficiaries;

    private List<IExtendedPersonDetailsForm> directors;

    private JsonObjectMapper mapper;

    @InjectMocks
    private IndividualDtoConverter individualDtoConverter;

    private IClientApplicationForm defaultFormdata;

    @Mock
    FeatureTogglesService featureTogglesService;


    @Before
    public void setUp() throws IOException {
        ApplicationContextProvider applicationContextProvider = new ApplicationContextProvider(null);
        applicationContextProvider.setApplicationContext(appContext);
        mapper = new JsonObjectMapper();
        Mockito.when(appContext.getBean(eq("jsonObjectMapper"), any(Class.class))).thenReturn(mapper);
        Mockito.when(appContext.getBean(eq("jsonObjectMapper"))).thenReturn(mapper);

        defaultFormdata = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("client_application_form_data_2.json"));
        extendedPersonDetailsForm = Iterables.getOnlyElement(defaultFormdata.getInvestors());
        when(staticService.loadCodeByUserId(eq(CodeCategory.COUNTRY), eq("AU"), any(ServiceErrors.class))).thenReturn(new CodeImpl("Australia", "Australia", "Australia"));
        when(addressDtoConverter.getAddressDto(any(IAddressForm.class), anyBoolean(), anyBoolean(), any(ServiceErrors.class))).thenReturn(new AddressDto());
        IClientApplicationForm formdataWithAdditional = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("client_application_corpsmsf_form_data_with_addl_members.json"));
        additionalShareholders = formdataWithAdditional.getAdditionalShareholdersAndMembers();
        firstAdditionalShareholder = additionalShareholders.get(0);
        IClientApplicationForm formdataWithMembers = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("individualSMSF_from_data_with_addl_members.json"));
        personFormWithTrustees = formdataWithMembers.getTrustees();
        additionalMembers = formdataWithMembers.getAdditionalShareholdersAndMembers();
        IClientApplicationForm formdataWithTrustees = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("trustInd_gcmret_with trustees.json"));
        TrustFormWithTrustees = formdataWithTrustees.getTrustees();
        additionalBeneficiaries = formdataWithTrustees.getAdditionalShareholdersAndMembers();
        IClientApplicationForm formdataWithOtherPhone = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("client_application_gcmret_individual_otherphone.json"));
        personDetailFormWithOtherPhone = Iterables.getOnlyElement(formdataWithOtherPhone.getInvestors());
        IClientApplicationForm formDataCorporateSmsf = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("client_application_corpsmsf_form_data_with_addl_members.json"));
        directors = formDataCorporateSmsf.getDirectors();
    }

    private IClientApplicationForm getClientApplicationForm(String jsonFile) throws IOException {
        Object formData = mapper.readValue(readJsonStringFromFile(jsonFile), OnboardingApplicationFormData.class);
        return ClientApplicationFormFactoryV1.getNewClientApplicationForm((OnboardingApplicationFormData) formData);
    }


    @Test
    public void convertFromIndividualTrustForm_shouldSetAddressV2() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_indtrust_formv2.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getTrustees());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(individualDto.getAddressesV2().get(0).getAddressDisplayText(),is("Unit 15  1 Clarence Street, STRATHFIELD  NSW  2135"));
        assertThat(individualDto.getAddressesV2().get(0).getAddressType(),is(AddressTypeV2.RESIDENTIAL));
        assertThat(individualDto.getAddressesV2().get(1).getAddressDisplayText(),is("Unit 15  1 Clarence Street, STRATHFIELD  NSW  2135"));
        assertThat(individualDto.getAddressesV2().get(1).getAddressType(),is(AddressTypeV2.POSTAL));
        IExtendedPersonDetailsForm additionalShareholders =  Iterables.getOnlyElement(clientApplicationForm.getAdditionalShareholdersAndMembers());
        IndividualDto shareholder = individualDtoConverter.convertFromIndividualForm(additionalShareholders, null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(shareholder.getAddressesV2().get(0).getAddressDisplayText(),is("Unit 15  1 Clarence Street, STRATHFIELD  NSW  2135"));
        assertThat(shareholder.getAddressesV2().get(0).getAddressType(),is(AddressTypeV2.RESIDENTIAL));
        verify(crsTaxDetailHelperService, times(2)).populateCRSTaxDetailsForIndividual(any(IPersonDetailsForm.class),any(InvestorDto.class));
    }


    @Test
    public void convertFromIndividualForm_shouldSetAddressV2andAddressV1() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_ind_intaddress_v2.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(individualDto.getAddressesV2().get(0).getAddressDisplayText(),is("Unit 15  1 Clarence Street, STRATHFIELD  NSW  2135"));
        assertThat(individualDto.getAddressesV2().get(0).getAddressType(),is(AddressTypeV2.POSTAL));
        assertNotNull(individualDto.getAddresses().get(0));
        verify(crsTaxDetailHelperService, times(1)).populateCRSTaxDetailsForIndividual(any(IPersonDetailsForm.class),any(InvestorDto.class));
    }


    @Test
    public void convertFromIndividualForm_shouldSetOtherPhones() {
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailFormWithOtherPhone, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(individualDto.getPhones().size(),is(2));
        assertThat(individualDto.getPhones().get(1).getPhoneType(),is("Other"));
    }

    @Test
    public void convertFromIndividualForm_shouldSetIDVVerifiesForTrutees() {
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personFormWithTrustees.get(0), null,IClientApplicationForm.AccountType.INDIVIDUAL_SMSF);
        assertThat(individualDto.getIdvs(),is("Verified"));
        IndividualDto memberDto = individualDtoConverter.convertFromIndividualForm(additionalMembers.get(0), null,IClientApplicationForm.AccountType.INDIVIDUAL_SMSF);
        assertThat(additionalMembers.size(), is(1));
        assertNull(memberDto.getIdvs());
        IndividualDto trusteeDto = individualDtoConverter.convertFromIndividualForm(TrustFormWithTrustees.get(0), null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(trusteeDto.getIdvs(),is("Verified"));
        IndividualDto beneficiaryDto = individualDtoConverter.convertFromIndividualForm(additionalBeneficiaries.get(0), null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertNull(beneficiaryDto.getIdvs());

    }

    @Test
    public void convertFromIndividualForm_shouldSetIndividualsDetails() throws IOException {
        this.extendedPersonDetailsForm = Iterables.getOnlyElement(this.defaultFormdata.getInvestors());
        when(staticService.loadCodeByUserId(eq(CodeCategory.PERSON_TITLE), eq("prof"), any(ServiceErrors.class))).thenReturn(new CodeImpl("id1", "userId", "Prof"));
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null,IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(dto.getTitle(), is("Prof"));
        assertThat(dto.getFullName(), is("NEMO A"));
        assertThat(dto.getFirstName(), is("NEMO"));
        assertThat(dto.getLastName(), is("A"));
        assertThat(dto.getDateOfBirth(), is("30 Jun 1960"));
        assertThat(dto.getGender(), is("Male"));
        verify(crsTaxDetailHelperService, times(0)).populateCRSTaxDetailsForIndividual(any(IPersonDetailsForm.class),any(InvestorDto.class));
    }

    @Test
    public void convertFromIndividualForm_shouldSetTitleFromStaticData() {
        CodeImpl codeImpl = Mockito.mock(CodeImpl.class);
        when(codeImpl.getName()).thenReturn("Mr");
        when(staticService.loadCodeByUserId(eq(CodeCategory.PERSON_TITLE), anyString(), any(ServiceErrors.class))).thenReturn(codeImpl);
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null,IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(dto.getTitle(), is("Mr"));
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetGenderWhenNotSpecified() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(firstAdditionalShareholder, null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertNull(dto.getGender());
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetTaxCountryCodeWhenNotSpecified() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(firstAdditionalShareholder, null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertNull(dto.getResiCountryforTax());
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetEmailWhenNotSpecified() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(firstAdditionalShareholder, null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertNull(dto.getEmails());
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetMobileWhenNotSpecified() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(firstAdditionalShareholder, null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertNull(dto.getPhones());
    }

    @Test
    public void convertFromIndividualForm_shouldCallAddressConverter() throws Exception {
        //we have to recreate new AddressDtoConverter so that Spring is not using the same one for each @Test
        addressDtoConverter = mock(AddressDtoConverter.class);
        individualDtoConverter.setAddressDtoConverter(addressDtoConverter);
        individualDtoConverter.convertFromIndividualForm(firstAdditionalShareholder, null,IClientApplicationForm.AccountType.INDIVIDUAL_TRUST );
        verify(addressDtoConverter, times(1)).getAddressDto(any(IAddressForm.class), anyBoolean(), anyBoolean(), any(ServiceErrors.class));
    }

    @Test
    public void convertFromIndividualForm_shouldSetRolesWhenInvestorIsAdditional() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(additionalShareholders.get(0), null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(dto.getPersonRoles().size(), is(1));
        assertThat(dto.getPersonRoles(), contains(InvestorRole.BeneficialOwner));
        dto = individualDtoConverter.convertFromIndividualForm(additionalShareholders.get(1), null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(dto.getPersonRoles().size(), is(1));
        assertThat(dto.getPersonRoles(), contains(InvestorRole.Member));
        dto = individualDtoConverter.convertFromIndividualForm(additionalShareholders.get(2), null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertThat(dto.getPersonRoles().size(), is(2));
        assertThat(dto.getPersonRoles(), contains(InvestorRole.Member, InvestorRole.BeneficialOwner));
        assertNull(dto.getPhones());
    }

    @Test
    public void convertFromIndividualForm_shouldSetPrimaryEmail() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        List<EmailDto> emails = dto.getEmails();
        EmailDto primaryEmail = emails.get(0);
        assertThat(primaryEmail.getEmail(), is("a@a.com"));
        assertThat(primaryEmail.getEmailType(), is("Primary"));
        assertThat(primaryEmail.isPreferred(), is(false));
    }

    @Test
    public void convertFromIndividualForm_shouldSetSecondaryEmail() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        List<EmailDto> emails = dto.getEmails();
        EmailDto secondaryEmail = emails.get(1);
        assertThat(secondaryEmail.getEmail(), is("b@b.au"));
        assertThat(secondaryEmail.getEmailType(), is("Secondary"));
        assertThat(secondaryEmail.isPreferred(), is(true));
    }

    @Test
    public void convertFromIndividualForm_shouldSetPrimaryPhoneDetails() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL );
        PhoneDto primaryMobile = dto.getPhones().get(0);
        assertThat(primaryMobile.getNumber(), is("2223333"));
        assertThat(primaryMobile.getPhoneType(), is("Primary"));
        assertThat(primaryMobile.isPreferred(), is(false));
    }

    @Test
    public void convertFromIndividualForm_shouldSetSecondaryPhoneDetails() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL );
        PhoneDto homeNumber = dto.getPhones().get(1);
        assertThat(homeNumber.getNumber(), is("444555"));
        assertThat(homeNumber.getPhoneType(), is("Home"));
        assertThat(homeNumber.isPreferred(), is(false));
    }

    @Test
    public void convertFromIndividualForm_shouldSetIdvDetails() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(dto.getIdvs(), is("Verified"));
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetIdvDetailsIfMemberOrBeneficiary() {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(additionalShareholders.get(1), null, IClientApplicationForm.AccountType.COMPANY);
        assertThat(dto.getPersonRoles().size(), is(1));
        assertThat(dto.getPersonRoles(), contains(InvestorRole.Member));
        assertNull(dto.getIdvs());
    }

    @Test
    public void convertFromIndividualForm_shouldConvertIndividualWhenAdditionalContactMethodsIsProvided() throws IOException {
        IClientApplicationForm formdata = ClientApplicationFormFactory.getNewClientApplicationForm(readJsonFromFile("client_application_form_data_3.json"));
        extendedPersonDetailsForm = Iterables.getOnlyElement(formdata.getInvestors());
        when(staticService.loadCodeByUserId(eq(CodeCategory.COUNTRY), eq("AW"), any(ServiceErrors.class))).thenReturn(new CodeImpl("AW", "AW", "AW"));
        when(staticService.loadCodeByUserId(eq(CodeCategory.COUNTRY), eq("CI"), any(ServiceErrors.class))).thenReturn(new CodeImpl("CI", "CI", "CI"));
        CodeImpl norfolkIslandResident = new CodeImpl("norfolk_island_res", "norfolk_island_res", "Norfolk island res", "norfolk_island_res");
        when(staticService.loadCodeByAvaloqId(eq(CodeCategory.EXEMPTION_REASON), anyString(), any(ServiceErrors.class))).thenReturn(norfolkIslandResident);
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(extendedPersonDetailsForm, null, IClientApplicationForm.AccountType.INDIVIDUAL );

        PhoneDto primaryMobile = dto.getPhones().get(0);
        assertThat(primaryMobile.getNumber(), is("45000777"));
        assertThat(primaryMobile.getPhoneType(), is("Primary"));
        assertThat(primaryMobile.isPreferred(), is(false));

        PhoneDto secondaryMobile = dto.getPhones().get(1);
        assertThat(secondaryMobile.getNumber(), is("867845453899"));
        assertThat(secondaryMobile.getPhoneType(), is("Secondary"));
        assertThat(secondaryMobile.isPreferred(), is(false));

        PhoneDto homeNumber = dto.getPhones().get(2);
        assertThat(homeNumber.getNumber(), is("65777"));
        assertThat(homeNumber.getPhoneType(), is("Home"));
        assertThat(homeNumber.isPreferred(), is(true));

        PhoneDto workNumber = dto.getPhones().get(3);
        assertThat(workNumber.getNumber(), is("4000"));
        assertThat(workNumber.getPhoneType(), is("Work"));
        assertThat(workNumber.isPreferred(), is(false));
    }

    @Test
    public void convert_ShouldSetMemberRoleForInvestor() {
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.isMember()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL );
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.Member));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Beneficiary)));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Shareholder)));
    }

    @Test
    public void convert_ShouldSetShareHolderRoleForInvestor() {
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.isShareholder()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.BeneficialOwner));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Beneficiary)));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Member)));
    }


    @Test
    public void convert_ShouldSetBeneficiaryRoleForInvestor(){
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.isBeneficiary()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.Beneficiary));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Shareholder)));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Member)));
    }

    @Test
    public void convert_ShouldSetShareholderAndBeneficiaryRoleForInvestor(){
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.isShareholder()).thenReturn(true);
        when(investor.isBeneficiary()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL );
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.Beneficiary));
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.BeneficialOwner));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Member)));
    }

    @Test
    public void convert_ShouldSetMemberAndShareholderForInvestor(){
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.isShareholder()).thenReturn(true);
        when(investor.isMember()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL);
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.Member));
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.BeneficialOwner));
        assertThat(investorDto.getPersonRoles(), not(hasItem(InvestorRole.Beneficiary)));
    }


    @Test
    public void convert_ShouldSetBeneficiaryForTheAccount(){
        IExtendedPersonDetailsForm investor = createBeneficiary();
        when(investor.isBeneficiary()).thenReturn(true);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        Map<String, Object> identityDocument = emptyMap();
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL);
        IndividualDto beneficiary = (IndividualDto)investorDto;
        assertThat(beneficiary.getFullName(), is("FirstName LastName"));
        assertThat(beneficiary.getTitle(), is("Mr"));
        Assert.assertNull(beneficiary.getDateOfBirth());
        assertThat(investorDto.getPersonRoles(), hasItem(InvestorRole.Beneficiary));
    }

    @Test
    public void convertShouldSetWorkNumber(){
        IExtendedPersonDetailsForm investor = createInvestor();
        when(investor.hasWorkNumber()).thenReturn(true);
        when(investor.hasMobile()).thenReturn(true);
        IContactValue contactValue = mock(IContactValue.class);
        when(contactValue.getAreaCode()).thenReturn("02");
        when(contactValue.getCountryCode()).thenReturn("61");
        when(contactValue.getValue()).thenReturn("85236985");

        IContactValue mobileContact = mock(IContactValue.class);
        when(mobileContact.getValue()).thenReturn("0411111111");
        when(investor.getMobile()).thenReturn(mobileContact);
        when(investor.getWorkNumber()).thenReturn(contactValue);
        IIdentityVerificationForm identityVerificationForm = mock(IIdentityVerificationForm.class);
        when(investor.getIdentityVerificationForm()).thenReturn(identityVerificationForm);
        InvestorDto investorDto = individualDtoConverter.convertFromIndividualForm(investor, new FailFastErrorsImpl(), IClientApplicationForm.AccountType.INDIVIDUAL );
        assertThat(investorDto.getPhones().get(1).getAreaCode(),is("02"));
        assertThat(investorDto.getPhones().get(1).getCountryCode(),is("61"));
        assertThat(investorDto.getPhones().get(1).getNumber(),is("85236985"));

    }

    @Test
    public void convertFromIndividualForm_shouldSetCompanySecretaryRoleForDirector() throws Exception {
        IndividualDto dto = individualDtoConverter.convertFromIndividualForm(directors.get(1), null, IClientApplicationForm.AccountType.CORPORATE_SMSF);
        assertThat(dto.getPersonRoles().size(), is(1));
        assertThat(dto.getPersonRoles(), contains(InvestorRole.Secretary));
    }

    @Test
    public void convertFromIndividualForm_shouldCallCRSUtility() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_crsData.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        verify(crsTaxDetailHelperService, times(1)).populateCRSTaxDetailsForIndividual(any(IPersonDetailsForm.class),any(InvestorDto.class));

    }

    @Test
    public void convertFromIndividualForm_shouldSetPersonRolesForCorporateTrust() throws IOException {
        IClientApplicationForm formData = getClientApplicationForm("client_application_corporate_trust_form_data_aml.json");
        IExtendedPersonDetailsForm additionalPerson = Iterables.getOnlyElement(formData.getAdditionalShareholdersAndMembers());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(additionalPerson, null, IClientApplicationForm.AccountType.CORPORATE_TRUST);
        assertTrue(individualDto.getPersonRoles().size() == 3);
        assertTrue(individualDto.getPersonRoles().containsAll(Arrays.asList(InvestorRole.Beneficiary, InvestorRole.BeneficialOwner, InvestorRole.ControllerOfTrust)));

        verify(crsTaxDetailHelperService, times(1)).populateCRSTaxDetailsForIndividual(any(IPersonDetailsForm.class),any(InvestorDto.class));

    }


    @Test
    public void convertFromIndividualForm_shouldSetTaxExemptionReason() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("individual_existing_cis_crs.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertFalse(individualDto.isTfnProvided());
        assertThat(individualDto.getExemptionReason(), is("Centrelink Benefits"));
    }

    @Test
    public void convertFromIndividualForm_ExemptionReason_NotProvided() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_crsData.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL_TRUST);
        assertFalse(individualDto.isTfnProvided());
        assertNull(individualDto.getExemptionReason());
    }

    @Test
    public void convertFromIndividualForm_shouldSetExemptionReason_ForPensioner() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_crsData_pension.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.SUPER_PENSION);
        assertFalse(individualDto.isTfnProvided());
        assertThat(individualDto.getExemptionReason(),is("Exempt as payee is a pensioner"));
    }


    @Test
    public void convertFromIndividualForm_shouldSetTFN() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_crsData_indv_tfn.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        assertTrue(individualDto.isTfnProvided());
        assertNull(individualDto.getExemptionReason());
    }

    @Test
    public void convertFromIndividualForm_shouldNotSetExemptionReason_Invalid() throws IOException {
        IClientApplicationForm clientApplicationForm = getClientApplicationForm("client_application_crsData_invalidExemptReason.json");
        IExtendedPersonDetailsForm personDetailForm = Iterables.getOnlyElement(clientApplicationForm.getInvestors());
        IndividualDto individualDto = individualDtoConverter.convertFromIndividualForm(personDetailForm, null, IClientApplicationForm.AccountType.INDIVIDUAL);
        assertFalse(individualDto.isTfnProvided());
        assertNull(individualDto.getExemptionReason());
    }

    private IExtendedPersonDetailsForm createInvestor() {
        XMLGregorianCalendar xmlGregorianCalendar = XMLGregorianCalendarUtil.getXMLGregorianCalendar("10/10/1990", "dd/MM/yyyy");
        IExtendedPersonDetailsForm investor = mock(IExtendedPersonDetailsForm.class);
        when(staticService.loadCodeByUserId(eq(CodeCategory.COUNTRY), eq("Any-Country"), any(ServiceErrors.class))).thenReturn(new CodeImpl("Any-Country", "Any-Country", "Any-Country"));
        when(investor.getTaxCountryCode()).thenReturn("Any-Country");
        when(investor.getTitle()).thenReturn("mr");
        when(investor.getDateOfBirthAsCalendar()).thenReturn(xmlGregorianCalendar);

        IAddressForm addressForm = mock(IAddressForm.class);
        when(addressForm.getAddressIdentifier()).thenReturn(null);

        when(investor.getResidentialAddress()).thenReturn(addressForm);
        when(investor.getPostalAddress()).thenReturn(addressForm);
        return investor;
    }

    private IExtendedPersonDetailsForm createBeneficiary() {
        IExtendedPersonDetailsForm investor = mock(IExtendedPersonDetailsForm.class);
        when(investor.getTitle()).thenReturn("mr");
        when(investor.getFirstName()).thenReturn("FirstName");
        when(investor.getLastName()).thenReturn("LastName");
        return investor;
    }
}
