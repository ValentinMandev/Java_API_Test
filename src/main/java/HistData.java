import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.external.api.transport.listeners.IGenericMessageListener;
import com.fxcm.external.api.transport.listeners.IStatusMessageListener;
import com.fxcm.fix.*;
import com.fxcm.fix.pretrade.MarketDataRequest;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.fxcm.fix.pretrade.TradingSessionStatus;
import com.fxcm.messaging.ISessionStatus;
import com.fxcm.messaging.ITransportable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class HistData
{
    private static List cAccounts = new ArrayList();
    private static String cAccountMassID = "";
    private static String cOpenOrderMassID = "";
    private static String cOpenPositionMassID = "";
    private static String cClosedPositionMassID = "";
    private static String cTradingSessionStatusID = "";
    private static TradingSessionStatus cTradingSessionStatus;
    private static boolean cPrintMarketData;
    private static int historyData = 0;
    private static UTCDate lastDate, firstDate;
    private static UTCTimeOnly lastTime, firstTime;
    private static int maxRec = 300;
    private static int rec1, rec2, rec3, dispRec;
    private static UTCDate dateRange[][] = new UTCDate[2][1000];
    private static UTCTimeOnly timeRange[][] = new UTCTimeOnly[2][1000];



    public static void main(String[] args)
    {
        final IGateway fxcmGateway = GatewayFactory.createGateway();
        fxcmGateway.registerGenericMessageListener(new IGenericMessageListener()
        {
            public void messageArrived(ITransportable aMessage)
            {
                if (aMessage instanceof MarketDataSnapshot)
                {
                    MarketDataSnapshot aMarketDataSnapshot = (MarketDataSnapshot) aMessage;
                    if (aMarketDataSnapshot.getMDReqID() != null)
                    {
                        if (dispRec == 1 && ((MarketDataSnapshot) aMessage).getQuoteID() == null)
                            System.out.println("BAR Time: "+aMarketDataSnapshot.getCloseTimestamp()+"  AskClose: " +aMarketDataSnapshot.getAskClose()+"  BidClose: " +aMarketDataSnapshot.getBidClose()
                                    +"  AskHigh: " +aMarketDataSnapshot.getAskHigh()+"  BidHigh: " +aMarketDataSnapshot.getBidHigh()
                                    +"  AskLow: " +aMarketDataSnapshot.getAskLow()+"  BidLow: " +aMarketDataSnapshot.getBidLow());

                        if (historyData == 2)
                        {
                            firstDate = aMarketDataSnapshot.getDate();
                            firstTime = aMarketDataSnapshot.getTime();
                            rec3++;
                            dateRange[0][rec3] = firstDate;
                            timeRange[0][rec3] = firstTime;
                            historyData = 0;
                        }
                        if (historyData == 0)
                        {
                            lastDate = aMarketDataSnapshot.getDate();
                            lastTime = aMarketDataSnapshot.getTime();
                            dateRange[1][rec3] = aMarketDataSnapshot.getDate();
                            timeRange[1][rec3] = aMarketDataSnapshot.getTime();
                        }
                    }
                }
                if (aMessage instanceof TradingSessionStatus)
                {
                    cTradingSessionStatus = (TradingSessionStatus) aMessage;
                    if (cTradingSessionStatusID.equals(cTradingSessionStatus.getRequestID()))
                    {
                        try {
                            MarketDataRequest mdr = new MarketDataRequest();
                            Enumeration securities = cTradingSessionStatus.getSecurities();
                            while (securities.hasMoreElements()) {
                                TradingSecurity o = (TradingSecurity) securities.nextElement();
                                mdr.addRelatedSymbol(o);
                            }
                            mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
                            mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
                            fxcmGateway.sendMessage(mdr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        fxcmGateway.registerStatusMessageListener(new IStatusMessageListener()
        {
            public void messageArrived(ISessionStatus aStatus)
            {
                switch (aStatus.getStatusCode())
                {
                    case ISessionStatus.STATUSCODE_READY:
                    case ISessionStatus.STATUSCODE_SENDING:
                    case ISessionStatus.STATUSCODE_RECIEVING:
                    case ISessionStatus.STATUSCODE_PROCESSING:
                    case ISessionStatus.STATUSCODE_WAIT:
                        break;
                    default:
                        System.out.println((
                                "client: inc status msg = ["
                                        + aStatus.getStatusCode()
                                        + "] ["
                                        + aStatus.getStatusName()
                                        + "] ["
                                        + aStatus.getStatusMessage()
                                        + "]").toUpperCase());
                        if (aStatus.getStatusCode() == ISessionStatus.STATUSCODE_DISCONNECTED) {
                            try {
                                fxcmGateway.relogin();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
            }
        });

        try
        {
            dispRec = 0;
            String username = "ValioDemo";
            String password = "1234";
            String terminal = "Demo";
            String server = "http://fxcorporate.com/Hosts.jsp";

            if (args.length == 4)
            {
                username = args[0];
                password = args[1];
                terminal = args[2];
                server = args[3];
            }
            String file = null;

            FXCMLoginProperties properties;
            properties = new FXCMLoginProperties(username, password, terminal, server);

            System.out.println("client: start logging in");
            fxcmGateway.login(properties);

            cTradingSessionStatusID = fxcmGateway.requestTradingSessionStatus();
            cAccountMassID = fxcmGateway.requestAccounts();
            System.out.println("client: done logging in\n");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            Thread.sleep(1000);

            GetHistory1("NAS100", fxcmGateway);
            Thread.sleep(500);

            fxcmGateway.logout();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static private int GetHistory1(String symbol, IGateway fxcmGateway)
    {
        try
        {
            // Construct new MarketDataRequest
            MarketDataRequest mdr = new MarketDataRequest();

            Enumeration securities = cTradingSessionStatus.getSecurities();
            while (securities.hasMoreElements())
            {
                TradingSecurity ts = (TradingSecurity) securities.nextElement();
                if (ts.getSymbol().equals(symbol))
                {
                    mdr.addRelatedSymbol(ts);
                }
            }

            mdr.setFXCMTimingInterval(new FXCMTimingIntervalFactory().DAY1);
            mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
            mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
            mdr.setResponseFormat(IFixDefs.MSGTYPE_FXCMRESPONSE);

            UTCDate date1 = new UTCDate("20230201");
            UTCTimeOnly time1 = new UTCTimeOnly("22:00:00");
            UTCDate date2 = new UTCDate("20230207");
            UTCTimeOnly time2 = new UTCTimeOnly("21:00:00");
            UTCDate date3 = new UTCDate(date1);
            UTCTimeOnly time3 = new UTCTimeOnly(time1);
            String s1 = new String("");
            String s2 = new String("");
            historyData = 2;
            rec3 = -1;
            while(true)
            {
                if (date2.toStringDateOnly().compareTo(date1.toStringDateOnly()) <= 0 ||
                        (date2.toStringDateOnly().compareTo(date1.toStringDateOnly()) <= 0 && time2.compareTo(time1) <= 0))
                    break;

                dispRec = 1;
                historyData = 2;

                if (date1 != null && time1 != null)
                {
                    mdr.setFXCMStartDate(date1);
                    mdr.setFXCMStartTime(time1);

                    mdr.setFXCMEndDate(date2);
                    mdr.setFXCMEndTime(time2);

                    fxcmGateway.sendMessage(mdr);
                }
                Thread.sleep(500);

                date2 = firstDate;
                time2 = firstTime;
                date1 = date3;
                time1 = time3;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            fxcmGateway.logout();
            System.exit(0);

        }
        return(0);
    }
}
