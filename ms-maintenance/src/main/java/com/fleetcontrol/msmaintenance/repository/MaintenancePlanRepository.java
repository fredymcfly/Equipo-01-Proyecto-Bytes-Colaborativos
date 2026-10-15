package com.fleetcontrol.msmaintenance.repository;

import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for maintenance plans. */
public interface MaintenancePlanRepository
    extends JpaRepository<MaintenancePlan, UUID>, JpaSpecificationExecutor<MaintenancePlan> {

  boolean existsByVehicleIdAndTypeAndActiveTrue(UUID vehicleId, MaintenanceType type);

  /** Active plans that have no order waiting or in progress. */
  default List<MaintenancePlan> findActiveWithoutOpenOrder() {
    return findActiveWithoutOrderOtherThan(MaintenanceOrderStatus.COMPLETED);
  }

  /** Active plans without any order whose status is different from the given one. */
  @Query(
      "select p from MaintenancePlan p where p.active = true and not exists ("
          + "select o.id from MaintenanceOrder o where o.planId = p.id "
          + "and o.status <> :closedStatus)")
  List<MaintenancePlan> findActiveWithoutOrderOtherThan(
      @Param("closedStatus") MaintenanceOrderStatus closedStatus);
}
