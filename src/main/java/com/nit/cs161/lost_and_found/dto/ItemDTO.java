package com.nit.cs161.lost_and_found.dto;

import com.nit.cs161.lost_and_found.constant.general.ProjectConstants;
import com.nit.cs161.lost_and_found.entity.laf.LafItem;
import com.nit.cs161.lost_and_found.utility.BaseDTO;
import com.nit.cs161.lost_and_found.utility.DateGenerator;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Descriptions: 物品传输类<p>
 *
 * @author Jin
 * @date 2018/10/6 13:32
 */
public class ItemDTO extends BaseDTO<LafItem> {
    private Integer itemId;
    private String itemName;
    private String itemDesc;
    private String itemPickUpTime;

    public ItemDTO() {
        super(LafItem.class);
    }

    public ItemDTO(LafItem bean) {
        super(LafItem.class);
        BeanUtils.copyProperties(bean, this);
        this.setItemPickUpTime(new DateGenerator(bean.getItemPickUpTime()).toString());
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
    }

    public String getItemPickUpTime() {
        return itemPickUpTime;
    }

    public void setItemPickUpTime(String itemPickUpTime) {
        this.itemPickUpTime = itemPickUpTime;
    }

    @Override
    public LafItem toBean() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        LafItem lafItem = super.toBean();
        lafItem.setItemPickUpTime(new DateGenerator(ProjectConstants.DATE_TIME_FORMAT, this.getItemPickUpTime()).toTimestamp());
        return lafItem;
    }

    @Override
    public String toString() {
        return "ItemDTO{" +
                "itemId=" + itemId +
                ", itemDesc='" + itemDesc + '\'' +
                ", itemPickUpTime=" + itemPickUpTime +
                '}';
    }
}
