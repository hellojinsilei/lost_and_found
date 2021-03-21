package com.nit.cs161.lost_and_found.repository;

import com.nit.cs161.lost_and_found.entity.laf.LafItem;
import com.nit.cs161.lost_and_found.entity.laf.LafReturnLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Descriptions: 物品持久层<p>
 *
 * @author Jin
 * @date 2018/10/6 13:50
 */
@Repository
public interface ReturnLogRepository extends JpaRepository<LafReturnLog, Integer>, JpaSpecificationExecutor<LafReturnLog> {

    /**
     * Descriptions: 根据物品名称查询<p>
     *
     * @author Jin
     * @date 2018/10/6 13:50
     */
    List<LafReturnLog> findLafReturnLogByOwnerUserId(Integer userId);
}
