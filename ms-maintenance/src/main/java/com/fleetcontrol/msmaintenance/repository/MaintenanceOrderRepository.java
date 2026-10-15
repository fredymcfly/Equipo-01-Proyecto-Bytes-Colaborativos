package com.fleetcontrol.msmaintenance.repository;

import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for maintenance orders. */
public interface MaintenanceOrderRepository
    extends JpaRepository<MaintenanceOrder, UUID>, JpaSpecificationExecutor<MaintenanceOrder> {

  long countByVehicleIdAndStatus(UUID vehicleId, MaintenanceOrderStatus status);
}
