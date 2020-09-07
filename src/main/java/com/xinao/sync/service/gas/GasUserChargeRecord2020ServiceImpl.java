package com.xinao.sync.service.gas;

import com.xinao.sync.config.DataSource;
import com.xinao.sync.config.DataSourceEnum;
import com.xinao.sync.entity.gas.GasUserChargeRecord2020Entity;
import com.xinao.sync.mapper.gas.GasUserChargeRecord2020Mapper;
import com.xinao.sync.service.gas.GasUserChargeRecord2020Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author stuil
 * @since 2020-09-04
 */
@Service
@DataSource(DataSourceEnum.DB2)
public class GasUserChargeRecord2020ServiceImpl extends ServiceImpl<GasUserChargeRecord2020Mapper, GasUserChargeRecord2020Entity> implements GasUserChargeRecord2020Service {
    @Override
    public boolean saveOrUpdates(GasUserChargeRecord2020Entity entityList) {
        return this.saveOrUpdate(entityList);
    }
}
