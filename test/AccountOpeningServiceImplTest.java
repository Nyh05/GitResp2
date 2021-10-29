package jrx.anytxn.card.test;

import jrx.anytxn.biz.common.util.OrgNumberUtil;
import jrx.anytxn.biz.common.web.AnyTxnHttpResponse;
import jrx.anytxn.card.exception.AnyTxnCardException;
import jrx.anytxn.card.exception.AnyTxnCardRespCode;
import jrx.anytxn.card.exception.CardRepDetail;
import jrx.anytxn.card.service.impl.AccountOpeningServiceImpl;
import jrx.anytxn.card.service.impl.OpenCardParamCheckService;
import jrx.anytxn.core.card.dto.AccountOpeningDTO;
import jrx.anytxn.parameter.components.card.dto.CardFaceResDTO;
import jrx.anytxn.parameter.components.card.dto.CardIssueResDTO;
import jrx.anytxn.parameter.components.card.service.ICardFaceService;
import jrx.anytxn.parameter.components.card.service.ICardIssueService;
import jrx.anytxn.parameter.mapper.broadcast.components.card.ParmAnnualFeeruleMapper;
import jrx.anytxn.parameter.mapper.broadcast.components.card.ParmCardCurrencyInfoMapper;
import jrx.anytxn.parameter.model.components.card.ParmAnnualFeerule;
import jrx.anytxn.parameter.product.dto.CardProductInfoResDTO;
import jrx.anytxn.parameter.product.service.ICardProductInfoService;
import jrx.anytxn.parameter.system.dto.OrganizationInfoResDTO;
import jrx.anytxn.parameter.system.service.IOrganizationInfoService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class AccountOpeningServiceImplTest {
    private String oriStr = "111";
    private String productNumberStr = "222";
    private String openCardDefultStr = "Y";
    private MockMvc mockMvc;
    private AccountOpeningDTO record;
    @Before
    public void before() {
        new OrgNumberUtil();
        record = new AccountOpeningDTO();
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountOpeningService).build();
    }

    @Mock
    private IOrganizationInfoService organizationInfoService;

    @Mock
    private OpenCardParamCheckService openCardParamCheckService;

    @Mock
    private ICardProductInfoService cardProductInfoService;

    @InjectMocks
    private AccountOpeningServiceImpl accountOpeningService;

    @Test(expected = AnyTxnCardException.class)
    /**
     * @Description  AppType = 1
     * @param []
     * @return void
     */
    public void openCardCheckTest() {
        record.setAppType("1");
        accountOpeningService.openCardCheck(record);
    }

    @Test
    /**
     * @Description 机构信息不存在 orgInfo == null
     * @param []
     * @return void
     */
    public void openCardCheckTest2() {
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:" + CardRepDetail.OI_E.getCnMsg());
        }
    }

    @Test
    /**
     * @Description 卡产品不存在 prodCardInfo == null
     * @param []
     * @return void
     */
    public void openCardCheckTest3() {

        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:" + CardRepDetail.CP_E.getCnMsg());
        }
    }

    @Test
    /**
     * @Description 数据状态错误卡产品已失效 status =0
     * @param []
     * @return void
     */
    public void openCardCheckTest4() {
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(new OrganizationInfoResDTO());
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("0");
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_STATUS_ERR.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        }
    }

    @Test
    /**
     * @Description 检查NextProcessingDay < setDateStart 异常
     * @param []
     * @return void
     */
    public void openCardCheckTest5() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        //anyString()
        when(organizationInfoService.findOrganizationInfo(anyString())).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2022,1,1));
        cardProductInfoResDTO.setDateEnd(LocalDate.of(2023,1,1));
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_STATUS_ERR.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        }
    }

    @Test
    /**
     * @Description  检查NextProcessingDay > setDateEnd 异常
     * @param []
     * @return void
     */
    public void openCardCheckTest6() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateEnd(LocalDate.of(2021,1,1));
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_STATUS_ERR.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        }
    }

    //TODO 跳过check recode 方法的测试
    public void openCardCheckTest7() {

    }

    @Mock
    private ParmCardCurrencyInfoMapper parmCardCurrencyInfoMapper;

    @Test
    /**
     * @Description  验证卡产品币种关联信息表是否存在 cardCurrencyInfoList.isEmpty()
     * @param []
     * @return void
     */
    public void openCardCheckTest8() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        //setOpenCardDefault
        cardProductInfoResDTO.setOpenCardDefault(openCardDefultStr);
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:" + CardRepDetail.Q_CP_CC_E.getCnMsg());
        }
    }

    @Mock
    private ICardIssueService cardIssueService;

    @Test
    /**
     * @Description 发卡参数 =null
     * @param []
     * @return void
     */
    public void openCardCheckTest9() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        //setOpenCardDefault
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:" + CardRepDetail.CIP_E.getCnMsg());
        }
    }

    @Mock
    private ParmAnnualFeeruleMapper parmAnnualFeeruleMapper;

    @Test
    /**
     * @Description 年费参数 = null
     * @param []
     * @return void
     */
    public void openCardCheckTest10() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        //setOpenCardDefault
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        when(cardIssueService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardIssueResDTO());
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:卡产品null年费规则参数为空");
        }
    }

    @Mock
    private ICardFaceService cardFaceService;

    @Test
    public void openCardCheckTest11() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        //setOpenCardDefault
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        when(cardIssueService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardIssueResDTO());

        List<ParmAnnualFeerule> feeList =new ArrayList<>();
        feeList.add(new ParmAnnualFeerule());
        when(parmAnnualFeeruleMapper.selectByTableId(oriStr,null)).thenReturn(feeList);
        try {
            accountOpeningService.openCardCheck(record);
        } catch (AnyTxnCardException e) {
            Assert.assertEquals(e.getErrCode(), AnyTxnCardRespCode.D_NOT_EXIST.getCode());
            Assert.assertEquals(e.getErrDetail(), "数据不存在:" + CardRepDetail.MCP_E.getCnMsg());
        }
    }

    @Test
    /**
     * @Description cardFace == null
     * @param []
     * @return void
     */
    public void openCardCheckTest12() {
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        //setOpenCardDefault
        when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        record.setOrganizationNumber(oriStr);
        record.setAppProductNumber(productNumberStr);
        when(cardIssueService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardIssueResDTO());
        List<ParmAnnualFeerule> feeList =new ArrayList<>();
        feeList.add(new ParmAnnualFeerule());
        when(parmAnnualFeeruleMapper.selectByTableId(anyString(),null)).thenReturn(feeList);
        when(cardFaceService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardFaceResDTO());
        AnyTxnHttpResponse<String> result = accountOpeningService.openCardCheck(record);
        Assert.assertEquals(result,AnyTxnHttpResponse.success("success"));
    }

    @Test(expected = Exception.class)
    /**
     * @Description  方法 try -catch 异常
     * @param []
     * @return void
     */
    public void openCardCheckTest13(){
        accountOpeningService.openCardCheck(null);
    }

    @Test
    /**
     * @Description 所有异常放在一起测试
     * @param []
     * @return void
     */
    public void openCardCheckTestAll() {
        //1
        record.setAppType("1");
        assertTest(AnyTxnCardRespCode.P_ERR.getCode(),"参数异常:"+CardRepDetail.CEN.getCnMsg());
        //2
        record.setAppType("2");
        assertTest(AnyTxnCardRespCode.D_NOT_EXIST.getCode(),"数据不存在:" + CardRepDetail.OI_E.getCnMsg());
        //3
        OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        when(organizationInfoService.findOrganizationInfo(anyString())).thenReturn(organizationInfoResDTO);
        record.setOrganizationNumber(oriStr);
        assertTest(AnyTxnCardRespCode.D_NOT_EXIST.getCode(),"数据不存在:" + CardRepDetail.CP_E.getCnMsg());
        //5
        CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        cardProductInfoResDTO.setStatus("0");
        record.setAppProductNumber(productNumberStr);
        when(cardProductInfoService.findByOrgAndProductNum(anyString(), anyString())).thenReturn(cardProductInfoResDTO);
        assertTest(AnyTxnCardRespCode.D_STATUS_ERR.getCode(),"数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        //6
        cardProductInfoResDTO.setStatus("1");
        cardProductInfoResDTO.setDateStart(LocalDate.of(2022,1,1));
        when(cardProductInfoService.findByOrgAndProductNum(anyString(), anyString())).thenReturn(cardProductInfoResDTO);
        assertTest(AnyTxnCardRespCode.D_STATUS_ERR.getCode(),"数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        //7
        cardProductInfoResDTO.setDateEnd(LocalDate.of(2021,1,1));
        when(cardProductInfoService.findByOrgAndProductNum(anyString(), anyString())).thenReturn(cardProductInfoResDTO);
        assertTest(AnyTxnCardRespCode.D_STATUS_ERR.getCode(),"数据状态错误:" + CardRepDetail.CP_I.getCnMsg());
        //OrganizationInfoResDTO organizationInfoResDTO = new OrganizationInfoResDTO();
        //organizationInfoResDTO.setNextProcessingDay(LocalDate.now());
        //when(organizationInfoService.findOrganizationInfo(oriStr)).thenReturn(organizationInfoResDTO);
        //CardProductInfoResDTO cardProductInfoResDTO = new CardProductInfoResDTO();
        //cardProductInfoResDTO.setStatus("1");
        //cardProductInfoResDTO.setDateStart(LocalDate.of(2020,1,1));
        //cardProductInfoResDTO.setDateStart(LocalDate.of(2023,1,1));
        ////setOpenCardDefault
        //when(cardProductInfoService.findByOrgAndProductNum(oriStr, productNumberStr)).thenReturn(cardProductInfoResDTO);
        //record.setOrganizationNumber(oriStr);
        //record.setAppProductNumber(productNumberStr);
        //when(cardIssueService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardIssueResDTO());
        //List<ParmAnnualFeerule> feeList =new ArrayList<>();
        //feeList.add(new ParmAnnualFeerule());
        //when(parmAnnualFeeruleMapper.selectByTableId(anyString(),null)).thenReturn(feeList);
        //when(cardFaceService.findByOrgAndTableId(oriStr,null)).thenReturn(new CardFaceResDTO());
        //AnyTxnHttpResponse<String> result = accountOpeningService.openCardCheck(record);
        //Assert.assertEquals(result,AnyTxnHttpResponse.success("success"));
    }

    private void assertTest(String errorCode,String Detail){
        try{
            accountOpeningService.openCardCheck(record);
        }catch (AnyTxnCardException e){
            Assert.assertEquals(e.getErrCode(), errorCode);
            Assert.assertEquals(e.getErrDetail(), Detail);
        }
    }
}