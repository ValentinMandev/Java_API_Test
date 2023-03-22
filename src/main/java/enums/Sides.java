package enums;

import com.fxcm.fix.ISide;
import com.fxcm.fix.SideFactory;

public enum Sides {

    BUY(SideFactory.BUY),
    SELL(SideFactory.SELL);

    private final ISide iSide;

    Sides(ISide iSide) {
        this.iSide = iSide;
    }

    public ISide getSide() {
        return this.iSide;
    }

}
