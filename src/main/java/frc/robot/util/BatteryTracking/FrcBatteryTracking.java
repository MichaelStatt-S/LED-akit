package frc.robot.util.BatteryTracking;

import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants;
import frc.robot.Robot;
import java.io.FileWriter;
import java.io.IOException;
import org.littletonrobotics.junction.Logger;

public class FrcBatteryTracking {
  private static final String batteryFilePath = "/home/lvuser/battery-id.txt";
  private final Alert reusedBatteryAlert =
      new Alert("Battery has not been changed since the last match.", Alert.AlertType.kWarning);

  private boolean hasWritten = false;
  private boolean haveBatteryData = false;
  /** Previous Battery's ID 0 = not attempted -1 = failed */
  private final int previousBatteryID = 0;

  private double batteryUsageAH = 0;
  private PowerDistribution powerDistribution = null;

  public FrcBatteryTracking(PowerDistribution powerDistribution) {
    if (Constants.currentMode != Constants.Mode.REAL) {
      return;
    }
    this.powerDistribution = powerDistribution;
    BatteryTracking.ERROR_HANDLER = new DriverStationLogger();
    BatteryTracking.initialRead();
    BatteryTracking.Battery insertedBattery = BatteryTracking.getInsertedBattery();
    if (insertedBattery == null) {
      new Alert("Failed to read battery data", Alert.AlertType.kError).set(true);
      return;
    }
    haveBatteryData = true;
    Logger.recordOutput("Battery/Id", insertedBattery.getId());
    Logger.recordOutput("Battery/Name", insertedBattery.getName());
    Logger.recordOutput("Battery/Year", insertedBattery.getName());
    Logger.recordOutput("Battery/InitialUsageAH", insertedBattery.getName());
    Logger.recordOutput(
        "Battery/LastUsed", insertedBattery.getLog().get(0).getDateTime().toString());
    BatteryTracking.updateAutonomously(this::usageSupplierAH);
    new Trigger(DriverStation::isDisabled)
        .onTrue(Commands.runOnce(BatteryTracking::manualAsyncUpdate));
  }

  public double usageSupplierAH() {
    return batteryUsageAH;
  }

  public void periodic() {
    if (Constants.currentMode != Constants.Mode.REAL) {
      return;
    }
    double current = powerDistribution.getTotalCurrent();
    batteryUsageAH += (current * (Robot.defaultPeriodSecs / (60 * 60)));
    if (haveBatteryData && DriverStation.isFMSAttached() && !hasWritten) {
      hasWritten = true;
      try {
        FileWriter fileWriter = new FileWriter(batteryFilePath);
        fileWriter.write(BatteryTracking.getInsertedBattery().getId());
        fileWriter.close();
      } catch (IOException e) {
        DriverStation.reportError(
            "Could not write battery file: " + e.getMessage(), e.getStackTrace());
      }
    }
  }

  private static class DriverStationLogger implements BatteryTracking.ProgramSpecificErrorHandling {

    @Override
    public void consumeError(String message, Exception e) {
      DriverStation.reportError(message, e.getStackTrace());
    }

    @Override
    public void consumeError(String message) {
      DriverStation.reportError(message, true);
    }

    @Override
    public void consumeWarning(String message, Exception e) {
      DriverStation.reportWarning(message, e.getStackTrace());
    }

    @Override
    public void consumeWarning(String message) {
      DriverStation.reportWarning(message, false);
    }
  }

  //  public FrcBatteryTracking() {
  //    BatteryTracking.initialRead();
  //    BatteryLogged autoLoggedBattery = (BatteryLogged) BatteryTracking.getInsertedBattery();
  //    Logger.processInputs("Battery", autoLoggedBattery);
  //    new Trigger(DriverStation::isDisabled)
  //        .onTrue(Commands.runOnce(BatteryTracking::manualAsyncUpdate));
  //  }

  //  public static class BatteryLogged extends BatteryTracking.Battery implements LoggableInputs {
  //
  //    private BatteryLogged(
  //        int id,
  //        String name,
  //        int year,
  //        double testedCapacityAH,
  //        List<LogEntry> log,
  //        double initialUsageAH,
  //        double sessionUsageAH,
  //        boolean hasNewLog) {
  //      super(id, name, year, testedCapacityAH, log, initialUsageAH, sessionUsageAH, hasNewLog);
  //    }
  //
  //    @Override
  //    public void toLog(LogTable table) {
  //      table.put("Name", name);
  //      table.put("Id", id);
  //      table.put("Year", year);
  //      table.put("TestedCapacityAH", testedCapacityAH);
  //      table.put("InitialUsageAH", initialUsageAH);
  //      table.put("SessionUsageAH", sessionUsageAH);
  //      table.put("HasNewLog", hasNewLog);
  //      table.put("LogEntries",log.toArray(new LoggableLogEntry[0]));
  //    }
  //
  //    @Override
  //    public void fromLog(LogTable table) {
  //      super.name = table.get("Name",name);
  //      super.id = table.get("Id",id);
  //      super.year = table.get("Year",year);
  //      super.testedCapacityAH = table.get("TestedCapacityAH",testedCapacityAH);
  //      super.initialUsageAH = table.get("InitialUsageAH",initialUsageAH);
  //      super.sessionUsageAH = table.get("SessionUsageAH",sessionUsageAH);
  //      super.hasNewLog = table.get("HasNewLog",hasNewLog);
  //      super.log = List.of(table.get("LogEntries", log.toArray(new LoggableLogEntry[0])));
  //    }
  //  }
  //
  //  public static class LoggableLogEntry extends BatteryTracking.Battery.LogEntry
  //      implements StructSerializable {
  //    // matching constructor because java
  //    public LoggableLogEntry(LocalDateTime dateTime, double usageAH) {
  //      super(dateTime, usageAH);
  //    }
  //
  //    public LogEntryStruct struct = new LogEntryStruct();
  //  }
  //
  //  public static class LogEntryStruct implements Struct<BatteryTracking.Battery.LogEntry> {
  //
  //    @Override
  //    public Class<BatteryTracking.Battery.LogEntry> getTypeClass() {
  //      return BatteryTracking.Battery.LogEntry.class;
  //    }
  //
  //    @Override
  //    public String getTypeName() {
  //      return "TrackedBatteryLogEntry";
  //    }
  //
  //    @Override
  //    public int getSize() {
  //      return kSizeInt64 + kSizeDouble;
  //    }
  //
  //    @Override
  //    public String getSchema() {
  //      return "long epochLocalTime;double usage";
  //    }
  //
  //    @Override
  //    public BatteryTracking.Battery.LogEntry unpack(ByteBuffer bb) {
  //      LocalDateTime dateTime = LocalDateTime.ofEpochSecond(bb.getLong(), 0, ZoneOffset.UTC);
  //      return new BatteryTracking.Battery.LogEntry(dateTime, bb.getDouble());
  //    }
  //
  //    @Override
  //    public void pack(ByteBuffer bb, BatteryTracking.Battery.LogEntry value) {
  //      bb.putLong(value.getDateTime().toEpochSecond(ZoneOffset.UTC));
  //      bb.putDouble(value.getUsageAH());
  //    }
  //  }
}
