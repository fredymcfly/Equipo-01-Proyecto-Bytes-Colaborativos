package com.fleetcontrol.msalerts.service.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.client.VehiclesClient;
import com.fleetcontrol.msalerts.models.dto.internal.VehicleDto;
import com.fleetcontrol.msalerts.support.FeignFailures;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the lookup of vehicle plates. */
@ExtendWith(MockitoExtension.class)
class PlateResolverTest {

  @Mock private VehiclesClient client;

  private PlateResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new PlateResolver(client);
  }

  @Test
  void lookup_returnsThePlateAndAsksOnlyOncePerVehicle() {
    UUID vehicleId = UUID.randomUUID();
    when(client.getVehicle(vehicleId)).thenReturn(new VehicleDto(vehicleId, "3342-HJK"));
    Function<UUID, String> lookup = resolver.newLookup();

    assertThat(lookup.apply(vehicleId)).isEqualTo("3342-HJK");
    assertThat(lookup.apply(vehicleId)).isEqualTo("3342-HJK");

    verify(client, times(1)).getVehicle(vehicleId);
  }

  @Test
  void lookup_whenTheServiceFails_returnsTheId() {
    UUID vehicleId = UUID.randomUUID();
    when(client.getVehicle(vehicleId)).thenThrow(FeignFailures.unavailable());

    assertThat(resolver.newLookup().apply(vehicleId)).isEqualTo(vehicleId.toString());
  }

  @Test
  void lookup_whenTheCircuitBreakerStopsTheCall_returnsTheId() {
    UUID vehicleId = UUID.randomUUID();
    when(client.getVehicle(vehicleId)).thenThrow(FeignFailures.circuitBroken());

    assertThat(resolver.newLookup().apply(vehicleId)).isEqualTo(vehicleId.toString());
  }

  @Test
  void lookup_withoutPlateInTheAnswer_returnsTheId() {
    UUID vehicleId = UUID.randomUUID();
    when(client.getVehicle(vehicleId)).thenReturn(new VehicleDto(vehicleId, null));

    assertThat(resolver.newLookup().apply(vehicleId)).isEqualTo(vehicleId.toString());
  }

  @Test
  void newLookup_startsWithAnEmptyCacheEachTime() {
    UUID vehicleId = UUID.randomUUID();
    when(client.getVehicle(vehicleId)).thenReturn(new VehicleDto(vehicleId, "3342-HJK"));

    resolver.newLookup().apply(vehicleId);
    resolver.newLookup().apply(vehicleId);

    verify(client, times(2)).getVehicle(vehicleId);
  }
}
