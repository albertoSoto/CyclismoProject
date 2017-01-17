package org.cowboycoders.ant.profiles.fitnessequipment;

/**
 * Created by fluxoid on 16/01/17.
 */
public class Capabilities {
    private boolean basicResistanceModeSupport;
    private Integer maximumResistance;
    private boolean simulationModeSupport;
    private boolean targetPowerModeSupport;

    protected Capabilities(boolean basicResistanceModeSupport, Integer maximumResistance, boolean simulationModeSupport, boolean targetPowerModeSupport) {
        this.basicResistanceModeSupport = basicResistanceModeSupport;
        this.maximumResistance = maximumResistance;
        this.simulationModeSupport = simulationModeSupport;
        this.targetPowerModeSupport = targetPowerModeSupport;
    }

    public boolean isBasicResistanceModeSupport() {
        return basicResistanceModeSupport;
    }

    public Integer getMaximumResistance() {
        return maximumResistance;
    }

    public boolean isSimulationModeSupport() {
        return simulationModeSupport;
    }

    public boolean isTargetPowerModeSupport() {
        return targetPowerModeSupport;
    }
}
