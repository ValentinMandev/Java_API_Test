package enums;

import com.fxcm.fix.ITimeInForce;
import com.fxcm.fix.TimeInForceFactory;

public enum TIFs {

    GTC(TimeInForceFactory.GOOD_TILL_CANCEL),
    FOK(TimeInForceFactory.FILL_OR_KILL),
    IOC(TimeInForceFactory.IMMEDIATE_OR_CANCEL),
    GTD(TimeInForceFactory.GOOD_TILL_DATE),
    DAY(TimeInForceFactory.DAY);

    private final ITimeInForce iTimeInForce;

    TIFs(ITimeInForce iTimeInForce) {
        this.iTimeInForce = iTimeInForce;
    }

    public ITimeInForce getTIF() {
        return this.iTimeInForce;
    }

}
