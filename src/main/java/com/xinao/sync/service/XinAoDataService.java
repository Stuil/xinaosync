package com.xinao.sync.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinao.sync.entity.gas.*;
import com.xinao.sync.entity.xinao.Card;
import com.xinao.sync.entity.xinao.Sale;
import com.xinao.sync.entity.xinao.UnitType;
import com.xinao.sync.entity.xinao.Users;
import com.xinao.sync.service.gas.*;
import com.xinao.sync.service.xinao.CardService;
import com.xinao.sync.service.xinao.SaleService;
import com.xinao.sync.service.xinao.UnitTypeService;
import com.xinao.sync.service.xinao.UsersService;
import com.xinao.sync.utils.MoneyUtil;
import com.xinao.sync.utils.SyncUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Lang;
import org.nutz.lang.Times;
import org.nutz.lang.util.NutMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @title: XinAoDataService
 * @description: 同步服务
 * @date: 2020/9/4
 * @author: zwh
 * @copyright: Copyright (c) 2020
 * @version: 1.0
 */
@Service
@Slf4j
public class XinAoDataService {

    static final String AREA_APPEND = "(新奥)";

    @Autowired
    UnitTypeService unitTypeService;

    @Autowired
    UsersService usersService;

    @Autowired
    CardService cardService;

    @Autowired
    SaleService saleService;

    @Autowired
    GasAreaCommunityService areaCommunityService;

    @Autowired
    GasBookNoService bookNoService;

    @Autowired
    GasUserService gasUserService;

    @Autowired
    GasMeterService meterService;

    @Autowired
    GasMendGasService mendGasService;

    @Autowired
    GasUserChargeRecordService recordService;

    @Autowired
    GasUserChargeRecord2018Service record2018Service;

    @Autowired
    GasUserChargeRecord2019Service record2019Service;

    @Autowired
    GasUserChargeRecord2020Service record2020Service;

    @Autowired
    GasRefundGasService refundGasService;

    // FIXME 测试改回卡  确认表类型种类

    // FIXME   总购气量总购气金额验证

    /**
     * @description: 同步方法
     * @date: 2020/9/4
     * @author: zwh
     */
    public void dataSync(Integer areaId) {
        try {
            AtomicInteger areaCount = new AtomicInteger();
            AtomicInteger bookCount = new AtomicInteger();
            AtomicInteger count = new AtomicInteger();
            AtomicInteger meterCount = new AtomicInteger();
            AtomicInteger buyGasCount = new AtomicInteger();
            AtomicInteger mendGasCount = new AtomicInteger();
            AtomicInteger refundGasCount = new AtomicInteger();
            List<GasUserChargeRecordEntity> chargeRecordList = new ArrayList<>();
            List<GasMendGasEntity> mendGasList = new ArrayList<>();
            List<GasRefundGasEntity> refundGasList = new ArrayList<>();
            // 获取小区
            boolean condition = areaId != null && areaId != 0;
            List<UnitType> unitList = unitTypeService.list(new QueryWrapper<UnitType>().eq(condition, "id", areaId).orderByAsc("id"));
            int XinAoConsumerCount = usersService.count(new QueryWrapper<Users>().eq(condition,"unit_no", areaId));
            unitList.forEach(unit -> {
                // 计算小区人数
                List<Users> usersList = usersService.list(new QueryWrapper<Users>().eq("unit_no", unit.getId()));

                GasAreaCommunityEntity areaCommunity = new GasAreaCommunityEntity();
                areaCommunity.setId(getUUID());
                areaCommunity.setName(unit.getName() + AREA_APPEND);
                areaCommunity.setAddress(areaCommunity.getName());
                areaCommunity.setOldKeywords(areaCommunity.getName());
                areaCommunity.setOpBy("system import");
                areaCommunity.setOpAt(Times.getTS());
                areaCommunity.setDelFlag(false);
                areaCommunity.setAreaCode("150203");
                // 是否存在小区
                GasAreaCommunityEntity commNames = areaCommunityService.getOnes(areaCommunity.getName());
                if (Lang.isEmpty(commNames)) {
                    boolean areaComm = areaCommunityService.saves(areaCommunity);
                    if (!areaComm) {
                        log.error("新增失败:{}", areaCommunity.getId());
                    } else {
                        areaCount.getAndIncrement();
                    }
                } else {
                    areaCommunity.setId(commNames.getId());
                    boolean areaComm = areaCommunityService.updates(areaCommunity);
                    if (!areaComm) {
                        log.error("更新失败:{}", areaCommunity.getId());
                    } else {
                        areaCount.getAndIncrement();
                    }
                }
                // 查询户号 按地址查 适合 金凤、新奥
                GasBookNoEntity bookOne = bookNoService.getOnes(new QueryWrapper<GasBookNoEntity>()
                        .eq("address", areaCommunity.getName()));
                GasBookNoEntity bookNoEntity = new GasBookNoEntity();
                if (Lang.isEmpty(bookOne)) {
                    GasBookNoEntity books = bookNoService.getOnes(new QueryWrapper<GasBookNoEntity>().lt("id", 7000).orderByDesc("id").last("limit 1"));
                    bookNoEntity.setId(books.getId() + 1);
                    bookNoEntity.setAddress(areaCommunity.getName());
                    bookNoEntity.setCommunityId(areaCommunity.getId());
                    bookNoEntity.setDistributed(true);
                    bookNoEntity.setOpBy("system import");
                    bookNoEntity.setOpAt(Times.getTS());
                    bookNoEntity.setDelFlag(false);
                    bookNoEntity.setUserCount(usersList.size());
                    // 户号
                    boolean book = bookNoService.saves(bookNoEntity);
                    if (!book) {
                        log.error("失败:{}", bookNoEntity.getId());
                    } else {
                        bookCount.getAndIncrement();
                    }
                } else {
                    SyncUtil.copyProperties(bookOne, bookNoEntity);
                    bookNoEntity.setCommunityId(areaCommunity.getId());
                    bookNoEntity.setUserCount(unitList.size());
                    // 户号
                    boolean book = bookNoService.updates(bookNoEntity);
                    if (!book) {
                        log.error("失败:{}", bookNoEntity.getId());
                    } else {
                        bookCount.getAndIncrement();
                    }
                }

                // 人员同步
                usersList.forEach(users -> {
                    // 补卡次数
                    int mendCard = cardService.count(new QueryWrapper<Card>().eq("id", users.getId()));

                    // 交易记录
                    List<Sale> userAcc = saleService.list(new QueryWrapper<Sale>().eq("id", users.getId())
                            .and(i -> i.ne("amount", 0).or().ne("cost", 0)).orderByAsc("buy_date"));

                    // 购气记录
                    List<Sale> buyAcc = saleService.list(new QueryWrapper<Sale>().ne("id", users.getId())
                            .gt("amount", 0).gt("cost", 0).ne("updated", 1)
                            .orderByAsc("buy_date"));

                    // 补气记录
                    List<Sale> mendAcc = saleService.list(new QueryWrapper<Sale>().ne("id", users.getId())
                            .gt("amount", 0).ne("updated", 3).orderByAsc("buy_date"));

                    // 退气记录
                    List<Sale> refundAcc = saleService.list(new QueryWrapper<Sale>().ne("id", users.getId())
                            .lt("amount", 0).orderByAsc("buy_date"));

                    // 上次充值时间、金额
                    long lastRecTime = 0L;
                    long lastRecMoney = 0L;

                    // 总购气量
                    int buyCount = 0;
                    // 总购气金额
                    long buyMoney = 0L;

                    if (buyAcc != null && buyAcc.size() > 0) {
                        lastRecTime = buyAcc.get(buyAcc.size() - 1).getBuyDate().toEpochSecond(ZoneOffset.of("+8"));
                        lastRecMoney = MoneyUtil.yuanToLi(String.valueOf(buyAcc.get(buyAcc.size() - 1).getCost()));
                        buyCount = buyAcc.stream().mapToInt(Sale::getAmount).sum();
                        buyMoney = MoneyUtil.yuanToLi(String.valueOf(buyAcc.stream().mapToDouble(Sale::getCost).sum()));
                    }

                    // 总退气量
                    int refundCount = 0;
                    // 总退气金额
                    long refundMoney = 0L;
                    if (refundAcc != null && refundAcc.size() > 0) {
                        refundCount = refundAcc.stream().mapToInt(Sale::getAmount).sum();
                        refundMoney = MoneyUtil.yuanToLi(String.valueOf(refundAcc.stream().mapToDouble(Sale::getCost).sum()));
                    }

                    // 累计购气量
                    int sumBuyGas = buyCount + refundCount;
                    // fixme 累计购气金额
                    long sumBuyMoney = buyMoney + refundMoney;

                    // 用户信息
                    GasUserEntity gasUser = new GasUserEntity();
                    gasUser.setAccountNumber("XA" + SyncUtil.addZeroForNum(String.valueOf(users.getId()), 8));
                    gasUser.setSysNumber("04");
                    gasUser.setOldAccountNumber("");
                    gasUser.setCommunity(areaCommunity.getId());
                    gasUser.setBookNo(String.valueOf(bookNoEntity.getId()));
                    gasUser.setAccountNo(0);//补户号
                    gasUser.setAccountName(users.getName());
                    // 新奥无余额
                    gasUser.setBalance(0L);
                    gasUser.setArrears(0L);
                    gasUser.setLateFee(0L);
                    //用气类型
                    gasUser.setUseType(xinao_use_type_map.get(users.getUserType()));
                    gasUser.setMobile(users.getPhone());
                    gasUser.setAddress(users.getAddress());
                    gasUser.setUserType(1);
                    gasUser.setMeterId(getUUID());
                    gasUser.setOpenTime(users.getOpenDate().toEpochSecond(ZoneOffset.of("+8")));
                    gasUser.setAddGas(BigDecimal.valueOf(0));
                    gasUser.setStatus(1);
                    gasUser.setDelFlag(false);
                    gasUser.setCanDelete(false);
                    gasUser.setHeatingBand(0);
                    gasUser.setBatchOpen(0);
                    gasUser.setLastRechargeTime(lastRecTime);//上次充值时间
                    gasUser.setLastRechargeMoney(lastRecMoney);//上次充值金额
                    gasUser.setOpenCard(1);
                    gasUser.setTotalDosage(BigDecimal.valueOf(sumBuyGas));
                    // fixme 购气金额待测试
                    gasUser.setTotalBuyGasMoney(sumBuyMoney);
                    gasUser.setOpAt(Times.getTS());
                    gasUser.setOpByName("系统");
                    gasUser.setOpBy("system import");

                    GasUserEntity gasUserEntity = gasUserService.getByAcc(gasUser.getAccountNumber());

                    if (Lang.isEmpty(gasUserEntity)) {
                        //  用户入库
                        boolean user = gasUserService.insetUser(gasUser);
                        if (!user) {
                            log.error("失败:{}", gasUser.getAccountNumber());
                        } else {
                            count.getAndIncrement();
                        }
                    } else {
                        //  用户更新
                        gasUser.setMeterId(gasUserEntity.getMeterId());
                        boolean user = gasUserService.updateUser(gasUser);
                        if (!user) {
                            log.error("失败:{}", gasUser.getAccountNumber());
                        } else {
                            count.getAndIncrement();
                        }
                    }

                    // 表具信息
                    GasMeterEntity gasMeter = new GasMeterEntity();
                    gasMeter.setId(gasUser.getMeterId());
                    gasMeter.setMeterNo("");
                    gasMeter.setSupplier("XINAO");
                    // fixme  平台创建
                    gasMeter.setModelId("de22710d9c0e46ceb5433aac577a07e0");
                    gasMeter.setDigit(0);
                    gasMeter.setDelFlag(false);
                    gasMeter.setCardNo(String.valueOf(users.getId()));
                    gasMeter.setInstallTime(users.getOpenDate().toEpochSecond(ZoneOffset.of("+8")));
                    gasMeter.setOpenCard(1);
                    gasMeter.setOpenCardTime(users.getOpenDate().toEpochSecond(ZoneOffset.of("+8")));
                    gasMeter.setBuyGasCount(buyAcc.size() - refundAcc.size() + mendAcc.size());
                    gasMeter.setMendCardCount(mendCard);
                    gasMeter.setOpAt(Times.getTS());
                    gasMeter.setOpByName("系统");
                    gasMeter.setOpBy("system import");

                    boolean meter = meterService.insertMeter(gasMeter);
                    if (!meter) {
                        log.error("表具：{}", gasMeter.getId());
                    } else {
                        meterCount.getAndIncrement();
                    }

                    /*   交易记录   */
                    // 购气次数
                    int countGas = 0;
                    // 总购气量
                    int countGasSum = 0;
                    // 总购金额
                    Double countMoney = 0D;
                    for (Sale sale : userAcc) {
                        // 购气
                        if (sale.getUpdated() == 1 && sale.getAmount() > 0 && sale.getCost() > 0) {
                            GasUserChargeRecordEntity gasUserCharge = new GasUserChargeRecordEntity();
                            gasUserCharge.setId(SyncUtil.accNo(sale.getBuyDate(), Integer.valueOf(sale.getFlowid())));
                            gasUserCharge.setAccountNumber(gasUser.getAccountNumber());
                            gasUserCharge.setMeterId(gasUser.getMeterId());
                            gasUserCharge.setPaidAmount(MoneyUtil.yuanToLi(String.valueOf(sale.getCost())));
                            gasUserCharge.setChangeMoney(0L);
                            gasUserCharge.setArrears(0L);
                            //  新奥不计余额,置0
                            gasUserCharge.setBalance(0L);
                            gasUserCharge.setUserNewMoney(0L);
                            gasUserCharge.setPreStored(Long.valueOf(0));
                            gasUserCharge.setPayMethod(0);
                            gasUserCharge.setPaySource(2);
                            gasUserCharge.setUseType(gasUser.getUseType());
                            gasUserCharge.setDelFlag(false);
                            gasUserCharge.setUserType(1);
                            gasUserCharge.setBuyGas(BigDecimal.valueOf(sale.getAmount()));
                            // 购气时补气量
                            gasUserCharge.setMendGas(BigDecimal.valueOf(0));
                            gasUserCharge.setBuyGasAmount(MoneyUtil.yuanToLi(String.valueOf(sale.getCost())));
                            // 购气次数
                            countGas = countGas + 1;
                            gasUserCharge.setBuyGasTimes(Long.valueOf(countGas));
                            // 总购气量
                            countGasSum = countGasSum + sale.getAmount();
                            gasUserCharge.setTotalBuyGas(BigDecimal.valueOf(countGasSum));
                            // 总购金额
                            countMoney = countMoney + sale.getCost();
                            gasUserCharge.setTotalBuyGasAmount(MoneyUtil.yuanToLi(String.valueOf(countMoney)));

                            // 费用计算明细
                            long price = MoneyUtil.yuanToLi(String.valueOf(sale.getPrice()));//单价
                            NutMap feeDetailMap = NutMap.NEW().setv("ladder", false);
                            List<NutMap> ladderConfig = new ArrayList<>();
                            ladderConfig.add(NutMap.NEW().setv("price", price).setv("dosage", ""));
                            ladderConfig.add(NutMap.NEW());
                            ladderConfig.add(NutMap.NEW());
                            ladderConfig.add(NutMap.NEW());
                            ladderConfig.add(NutMap.NEW());
                            feeDetailMap.setv("ladderConfig", ladderConfig);
                            feeDetailMap.setv("gasValue", gasUserCharge.getBuyGas());
                            List<NutMap> detail = new ArrayList<>();
                            detail.add(NutMap.NEW().setv("price", price).setv("cur", 0).setv("dosage", gasUserCharge.getBuyGas()).setv("fee", gasUserCharge.getBuyGasAmount()));
                            feeDetailMap.setv("detail", detail);
                            feeDetailMap.setv("fee", gasUserCharge.getBuyGasAmount());
                            gasUserCharge.setFeeDetail(JSON.toJSONString(feeDetailMap));
                            gasUserCharge.setPayStatus(2);
                            gasUserCharge.setPayTime(sale.getBuyDate().toEpochSecond(ZoneOffset.of("+8")));
                            gasUserCharge.setStatus(0);
                            gasUserCharge.setDelFlag(false);
                            gasUserCharge.setSettleStatus(1);
                            gasUserCharge.setSettleTime(sale.getBuyDate().toEpochSecond(ZoneOffset.of("+8")));
                            gasUserCharge.setSettleByName("系统");
                            gasUserCharge.setOpByName("系统");
                            gasUserCharge.setOpAt(Times.getTS());
                            gasUserCharge.setOpBy("system import");

                            //购气入库
                            if (("2018").equals(sale.getBuyDate().toString().substring(0, 4))) {
                                GasUserChargeRecord2018Entity uc2018 = new GasUserChargeRecord2018Entity();
                                SyncUtil.copyProperties(gasUserCharge, uc2018);
                                boolean uc1 = record2018Service.saveOrUpdates(uc2018);
                                if (!uc1) {
                                    log.error("表具：{}", JSONObject.toJSONString(uc2018));
                                } else {
                                    buyGasCount.getAndIncrement();
                                }
                            } else if (("2019").equals(sale.getBuyDate().toString().substring(0, 4))) {
                                GasUserChargeRecord2019Entity uc2019 = new GasUserChargeRecord2019Entity();
                                SyncUtil.copyProperties(gasUserCharge, uc2019);
                                boolean uc2 = record2019Service.saveOrUpdates(uc2019);
                                if (!uc2) {
                                    log.error("表具：{}", JSONObject.toJSONString(uc2019));
                                } else {
                                    buyGasCount.getAndIncrement();
                                }
                            } else if (("2020").equals(sale.getBuyDate().toString().substring(0, 4))) {
                                GasUserChargeRecord2020Entity uc2020 = new GasUserChargeRecord2020Entity();
                                SyncUtil.copyProperties(gasUserCharge, uc2020);
                                boolean uc2 = record2020Service.saveOrUpdates(uc2020);
                                if (!uc2) {
                                    log.error("表具：{}", JSONObject.toJSONString(uc2020));
                                } else {
                                    buyGasCount.getAndIncrement();
                                }
                            }
                            chargeRecordList.add(gasUserCharge);
                        }
                        // 补气
                        if (sale.getAmount() > 0 && sale.getUpdated() == 3) {
                            GasMendGasEntity gasMendGasEntity = new GasMendGasEntity();
                            gasMendGasEntity.setId(SyncUtil.accNo(sale.getBuyDate(), Integer.valueOf(sale.getFlowid())));
                            gasMendGasEntity.setAccountNumber(gasUser.getAccountNumber());
                            gasMendGasEntity.setMeterId(gasUser.getMeterId());
                            gasMendGasEntity.setUseType(gasUser.getUseType());
                            gasMendGasEntity.setUserType(gasUser.getUserType());
                            gasMendGasEntity.setMendGas(BigDecimal.valueOf(sale.getAmount()));
                            gasMendGasEntity.setMendGasAmount(0L);
                            long price = MoneyUtil.yuanToLi(String.valueOf(sale.getPrice()));//单价
                            gasMendGasEntity.setCostMoney(price * sale.getAmount());
                            //   补气记录总购次数
                            gasMendGasEntity.setBuyGasTimes(countGas);
                            gasMendGasEntity.setChargeType(0);
                            gasMendGasEntity.setFeeDetail("");
                            gasMendGasEntity.setDelFlag(false);
                            gasMendGasEntity.setCreatAt(sale.getBuyDate().toEpochSecond(ZoneOffset.of("+8")));
                            gasMendGasEntity.setStatus(2);
                            gasMendGasEntity.setReason(sale.getName() + sale.getAddress());
                            gasMendGasEntity.setOpByName("系统");
                            gasMendGasEntity.setOpAt(Times.getTS());
                            gasMendGasEntity.setOpBy("system import");
                            //                      // 补气入库
                            boolean insertMendGas = mendGasService.saveOrUpdates(gasMendGasEntity);
                            if (!insertMendGas) {
                                log.error("表具：{}", JSONObject.toJSONString(gasMendGasEntity));
                            } else {
                                mendGasCount.getAndIncrement();
                            }
                            mendGasList.add(gasMendGasEntity);
                        }
                        // 退气
                        if (sale.getAmount() < 0) {
                            GasRefundGasEntity gasRefundGasEntity = new GasRefundGasEntity();
                            gasRefundGasEntity.setId(SyncUtil.accNo(sale.getBuyDate(), Integer.valueOf(sale.getId())));
                            gasRefundGasEntity.setAccountNumber(gasUser.getAccountNumber());
                            gasRefundGasEntity.setMeterId(gasUser.getMeterId());
                            gasRefundGasEntity.setUseType(gasUser.getUseType());
                            gasRefundGasEntity.setUserType(gasUser.getUserType());
                            gasRefundGasEntity.setRefundGas(BigDecimal.valueOf(Math.abs(sale.getAmount())));
                            // 本次退费总量
                            gasRefundGasEntity.setRefundCountGas(BigDecimal.valueOf(Math.abs(sale.getAmount())));
                            gasRefundGasEntity.setRefundGasMoney(0L);
                            gasRefundGasEntity.setRefundMoney(Math.abs(MoneyUtil.yuanToLi(String.valueOf(sale.getCost()))));//气费金额
                            gasRefundGasEntity.setChargeType(0);
                            gasRefundGasEntity.setChargeRecordId("");
                            gasRefundGasEntity.setStatus(2);
                            gasRefundGasEntity.setDelFlag(false);
                            gasRefundGasEntity.setCreatAt(sale.getBuyDate().toEpochSecond(ZoneOffset.of("+8")));
                            gasRefundGasEntity.setOpByName("系统");
                            gasRefundGasEntity.setOpAt(Times.getTS());
                            gasRefundGasEntity.setOpBy("system import");

                            // 退气入库
                            boolean refund1 = refundGasService.saveOrUpdates(gasRefundGasEntity);
                            if (!refund1) {
                                log.error("表具：{}", JSONObject.toJSONString(gasRefundGasEntity));
                            } else {
                                refundGasCount.getAndIncrement();
                            }
                            refundGasList.add(gasRefundGasEntity);
                        }
                    }
                });
            });
            String res = "小区成功:" + areaCount + "本号成功:" + bookCount;
            String countArea = "小区总数:" + unitList.size() + "本号总数:" + unitList.size();
            log.info(res);
            log.info(countArea);
            log.info("用户成功{}条,水表成功{}条,购气成功{}条,补气成功{}条，退气成功{}条",
                    count, meterCount, buyGasCount, mendGasCount, refundGasCount);
            log.info("用户总{}条,水表总{}条,购气总{}条,补气总{}条，退气总{}条",
                    XinAoConsumerCount, XinAoConsumerCount, chargeRecordList.size(),
                    mendGasList.size(), refundGasList.size());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("异常:{}", e.getMessage());
        }
    }

    /**
     * @description: UUID
     * @author: zwh
     */
    public String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    // 新奥 用气类型
    private static Map<Integer, String> xinao_use_type_map = new HashMap() {
        {
            put(1, "F_M_1");//居民天然气2.06
            put(2, "F_G_4");// add  商业天然气 2.3
            put(3, "F_M_0");//居民煤气0.8
            put(4, "F_G_1");//公用煤气1.2
        }
    };
    // 新奥 单价
    private static Map<String, Long> xinao_price_map = new HashMap() {
        {
            put("F_M_0", 800);//居民煤气0.8
            put("F_M_1", 2060);//居民天然气2.06
            put("F_G_4", 2300);// add  商业天然气 2.3
            put("F_G_1", 1200);//公用煤气1.2
        }
    };
    // 是否公用户
    private static Map<String, Boolean> xinao_public_user = new HashMap() {
        {
            put("F_M_0", false);//居民煤气0.8
            put("F_M_1", false);//居民天然气2.06
            put("F_G_4", true);// add  商业天然气 2.3
            put("F_G_1", true);//公用煤气1.2
        }
    };
}
