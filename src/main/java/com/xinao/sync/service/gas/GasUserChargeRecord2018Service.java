package com.xinao.sync.service.gas;

import com.xinao.sync.entity.gas.GasUserChargeRecord2018Entity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xinao.sync.entity.gas.GasUserChargeRecord2019Entity;
import com.xinao.sync.entity.gas.GasUserChargeRecordEntity;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author stuil
 * @since 2020-09-04
 */
public interface GasUserChargeRecord2018Service extends IService<GasUserChargeRecord2018Entity> {
    boolean saveOrUpdates(GasUserChargeRecord2018Entity entityList);
}
