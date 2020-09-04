package com.xinao.sync.entity.xinao;

    import com.baomidou.mybatisplus.extension.activerecord.Model;
    import java.io.Serializable;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.experimental.Accessors;

/**
* <p>
    * 
    * </p>
*
* @author stuil
* @since 2020-09-04
*/
    @Data
        @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    public class UnitType extends Model<UnitType> {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private String name;

    private Integer updated;

    private Integer amount;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
