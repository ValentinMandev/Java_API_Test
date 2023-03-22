import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.external.api.transport.listeners.IGenericMessageListener;
import com.fxcm.external.api.transport.listeners.IStatusMessageListener;
import com.fxcm.external.api.util.MessageGenerator;
import com.fxcm.fix.SubscriptionRequestTypeFactory;
import com.fxcm.fix.TradingSecurity;
import com.fxcm.fix.other.BusinessMessageReject;
import com.fxcm.fix.posttrade.CollateralReport;
import com.fxcm.fix.posttrade.PositionReport;
import com.fxcm.fix.pretrade.MarketDataRequest;
import com.fxcm.fix.pretrade.TradingSessionStatus;
import com.fxcm.fix.trade.ExecutionReport;
import com.fxcm.fix.trade.OrderSingle;
import com.fxcm.messaging.ISessionStatus;
import com.fxcm.messaging.ITransportable;
import com.fxcm.util.Util;
import common.Order;
import enums.Sides;
import enums.TIFs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;


public class CreateMarketOrder {
    private String mAccountMassID;
    private IGateway mFxcmGateway;
    private final String mPassword;
    private final String mServer;
    private final String mStation;
    private IStatusMessageListener mStatusListener;
    private final String mUsername;
    private final Order order;

    public CreateMarketOrder(String[] aArgs) {
        mUsername = aArgs[0];
        mPassword = aArgs[1];
        mStation = aArgs[2];
        mServer = aArgs[3];
        order = getOrder(Arrays.stream(aArgs).skip(4).toArray(String[]::new));
    }

    private boolean doResult(final MessageTestHandler aMessageTestHandler) {
        new Thread(() -> setup(aMessageTestHandler, false)).start();
        int expiration = 20; //seconds
        while (!aMessageTestHandler.isSuccess() && expiration > 0) {
            try {
                Thread.sleep(1000);
                expiration--;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (aMessageTestHandler.isSuccess()) {
            System.out.println("done waiting.\nstatus = " + aMessageTestHandler.isSuccess());
        } else {
            System.err.println("done waiting.\nstatus = " + aMessageTestHandler.isSuccess());
        }
        mFxcmGateway.removeGenericMessageListener(aMessageTestHandler);
        mFxcmGateway.removeStatusMessageListener(mStatusListener);
        mFxcmGateway.logout();
        return aMessageTestHandler.isSuccess();
    }

    private void handleMessage(ITransportable aMessage, List aAccounts, MessageTestHandler aMessageTestHandler) {
        if (aMessage instanceof CollateralReport) {
            CollateralReport cr = (CollateralReport) aMessage;
            if (safeEquals(mAccountMassID, cr.getRequestID()) && aAccounts != null) {
                aAccounts.add(cr);
            }
            aMessageTestHandler.process(cr);
        } else if (aMessage instanceof ExecutionReport) {
            aMessageTestHandler.process((ExecutionReport) aMessage);
        } else if (aMessage instanceof BusinessMessageReject) {
            aMessageTestHandler.process((BusinessMessageReject) aMessage);
        } else if (aMessage instanceof PositionReport) {
            aMessageTestHandler.process((PositionReport) aMessage);
        }
    }

    private static Order getOrder(String[] aArgs) {
        return new Order(aArgs[0], Integer.parseInt(aArgs[1]), aArgs[2], aArgs[3]);
    }

    private static void runTest(String[] aArgs) {
        CreateMarketOrder createMarketOrder = new CreateMarketOrder(aArgs);
        createMarketOrder.testCreateTrueMarketOrder();
    }

    public static boolean safeEquals(String aString1, String aString2) {
        return !(aString1 == null || aString2 == null) && aString1.equals(aString2);
    }

    private void setup(IGenericMessageListener aGenericListener, boolean aPrintStatus) {
        try {
            if (mFxcmGateway == null) {
                // step 1: get an instance of IGateway from the GatewayFactory
                mFxcmGateway = GatewayFactory.createGateway();
            }
            /*
                step 2: register a generic message listener with the gateway, this
                listener in particular gets all messages that are related to the trading
                platform Quote,OrderSingle,ExecutionReport, etc...
            */
            mFxcmGateway.registerGenericMessageListener(aGenericListener);
            mStatusListener = new DefaultStatusListener(aPrintStatus);
            mFxcmGateway.registerStatusMessageListener(mStatusListener);
            if (!mFxcmGateway.isConnected()) {
                System.out.println("client: login");
                FXCMLoginProperties properties = new FXCMLoginProperties(mUsername, mPassword, mStation, mServer);
                /*
                    step 3: call login on the gateway, this method takes an instance of FXCMLoginProperties
                    which takes 4 parameters: username,password,terminal and server or path to a Hosts.xml
                    file which it uses for resolving servers. As soon as the login  method executes your listeners begin
                    receiving asynch messages from the FXCM servers.
                */
                mFxcmGateway.login(properties);
            }
            //after login you must retrieve your trading session status and get accounts to receive messages
            mFxcmGateway.requestTradingSessionStatus();
            mAccountMassID = mFxcmGateway.requestAccounts();
            mFxcmGateway.requestOpenPositions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean testCreateTrueMarketOrder() {
        String cem = Util.getCurrentlyExecutingMethod();
        System.out.println(cem);
        class GenericListener extends MessageTestHandler {
            private final List mAccounts = new ArrayList();
            private boolean mOrder = true;
            private String mRequestId;

            public void process(CollateralReport aCollateralReport) {
                if (mOrder) {
                    mOrder = false;
                    OrderSingle orderSingle = MessageGenerator.generateMarketOrder(
                            aCollateralReport.getAccount(),
                            order.getAmount(),
                            Sides.valueOf(order.getSide().toUpperCase()).getSide(),
                            order.getSymbol(),
                            "true market order test");
                    orderSingle.setTimeInForce(TIFs.valueOf(order.getTif().toUpperCase()).getTIF());
                    try {
                        mRequestId = mFxcmGateway.sendMessage(orderSingle);
                        System.out.println("client: good order requestid = " + mRequestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void messageArrived(ITransportable aMessage) {
                handleMessage(aMessage, mAccounts, this);
            }
        }

        return doResult(new GenericListener());
    }

    private static class DefaultStatusListener implements IStatusMessageListener {
        private final boolean mPrint;

        DefaultStatusListener(boolean aPrint) {
            mPrint = aPrint;
        }

        public void messageArrived(ISessionStatus aStatus) {
            if (mPrint) {
                System.out.println("aStatus = " + aStatus);
            }
            if (aStatus.getStatusCode() == ISessionStatus.STATUSCODE_ERROR
                    || aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTING
                    || aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTED) {
                System.out.println("aStatus = " + aStatus);
            }
        }
    }

    private abstract class MessageTestHandler implements IGenericMessageListener {
        private boolean mSuccess;
        protected TradingSessionStatus mTradingSessionStatus;

        public boolean isSuccess() {
            return mSuccess;
        }

        public void process(CollateralReport aCollateralReport) {
            System.out.println("client inc: aCollateralReport = " + aCollateralReport);
            if (mAccountMassID.equals(aCollateralReport.getRequestID()) && aCollateralReport.isLastRptRequested()) {
                try {
                    System.out.println("client out: do marketdatarequest for testing to get fast mds");
                    MarketDataRequest mdr = new MarketDataRequest();
                    Enumeration securities = mTradingSessionStatus.getSecurities();
                    while (securities.hasMoreElements()) {
                        TradingSecurity o = (TradingSecurity) securities.nextElement();
                        if (order.getSymbol().equals(o.getSymbol())) {
                            mdr.addRelatedSymbol(o);
                        }
                    }
                    mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SUBSCRIBE);
                    mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                    mFxcmGateway.sendMessage(mdr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void process(ExecutionReport aExecutionReport) {
            System.out.println("client inc: aExecutionReport = " + aExecutionReport);
        }

        public void process(BusinessMessageReject aBusinessMessageReject) {
            System.out.println("client inc: aBusinessMessageReject = " + aBusinessMessageReject);
        }

        public void process(PositionReport aPositionReport) {
            System.out.println("client inc: aPositionReport = " + aPositionReport);
        }

    }

    public static void main(String[] aArgs) {
        if (aArgs.length < 8) {
            System.out.println("must supply 4 arguments: username, password, station, hostname," +
                    "symbol, amount, side, time in force");
            return;
        }

        runTest(aArgs);
    }
}
