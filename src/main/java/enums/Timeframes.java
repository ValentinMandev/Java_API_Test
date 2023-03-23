package enums;

import com.fxcm.fix.FXCMTimingIntervalFactory;
import com.fxcm.fix.IFXCMTimingInterval;

public enum Timeframes {

    T1(FXCMTimingIntervalFactory.TICK),
    S10(FXCMTimingIntervalFactory.SEC10),
    M1(FXCMTimingIntervalFactory.MIN1),
    M5(FXCMTimingIntervalFactory.MIN5),
    M15(FXCMTimingIntervalFactory.MIN15),
    M30(FXCMTimingIntervalFactory.MIN30),
    H1(FXCMTimingIntervalFactory.HOUR1),
    H2(FXCMTimingIntervalFactory.HOUR2),
    H3(FXCMTimingIntervalFactory.HOUR3),
    H4(FXCMTimingIntervalFactory.HOUR4),
    H6(FXCMTimingIntervalFactory.HOUR6),
    H8(FXCMTimingIntervalFactory.HOUR8),
    D1(FXCMTimingIntervalFactory.DAY1),
    W1(FXCMTimingIntervalFactory.WEEK1),
    MONTH1(FXCMTimingIntervalFactory.MONTH1);

    private final IFXCMTimingInterval timeframe;

    Timeframes(IFXCMTimingInterval timeframe) {
        this.timeframe = timeframe;
    }

    public IFXCMTimingInterval getTimeframe() {
        return this.timeframe;
    }

}
