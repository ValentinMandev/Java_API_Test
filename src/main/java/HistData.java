import com.fxcm.external.api.transport.FXCMLoginProperties;
import com.fxcm.external.api.transport.GatewayFactory;
import com.fxcm.external.api.transport.IGateway;
import com.fxcm.fix.*;
import com.fxcm.fix.pretrade.MarketDataRequest;
import com.fxcm.fix.pretrade.MarketDataSnapshot;
import com.fxcm.fix.pretrade.TradingSessionStatus;
import com.fxcm.messaging.ISessionStatus;
import enums.Timeframes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;

public class HistData
{
    private static String cAccountMassID = "";

    private static String cTradingSessionStatusID = "";
    private static TradingSessionStatus cTradingSessionStatus;
    private static int historyData = 0;
    private static UTCDate lastDate, firstDate;
    private static UTCTimeOnly lastTime, firstTime;
    private static int rec3, dispRec;
    private static final UTCDate[][] dateRange = new UTCDate[2][1000];
    private static final UTCTimeOnly[][] timeRange = new UTCTimeOnly[2][1000];

    private static class Request {
        final String username;
        final String password;
        final String terminal;
        final String server;
        final String symbol;
        final String timeframe;
        final String startDate;
        final String startTime;
        final String endDate;
        final String endTime;

        Request(String[] aArgs) {
            this.username = aArgs[0];
            this.password = aArgs[1];
            this.terminal = aArgs[2];
            this.server = aArgs[3];
            this.symbol = aArgs[4];
            this.timeframe = aArgs[5];
            this.startDate = aArgs[6];
            this.startTime = aArgs[7];
            this.endDate = aArgs[8];
            this.endTime = aArgs[9];
        }
    }

    static Request request;

    public static void main(String[] args)
    {

        request = new Request(args);

        final IGateway fxcmGateway = GatewayFactory.createGateway();
        fxcmGateway.registerGenericMessageListener(aMessage -> {
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
        });

        fxcmGateway.registerStatusMessageListener(aStatus -> {
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
        });

        try
        {
            dispRec = 0;

            String file = null;

            FXCMLoginProperties properties;
            properties = new FXCMLoginProperties(request.username, request.password, request.terminal, request.server);

            System.out.println("client: start logging in");
            fxcmGateway.login(properties);

            cTradingSessionStatusID = fxcmGateway.requestTradingSessionStatus();
            cAccountMassID = fxcmGateway.requestAccounts();
            System.out.println("client: done logging in\n");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            Thread.sleep(1000);

            GetHistory1(request.symbol, fxcmGateway);
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

            mdr.setFXCMTimingInterval(Timeframes.valueOf(request.timeframe.toUpperCase()).getTimeframe());
            mdr.setSubscriptionRequestType(SubscriptionRequestTypeFactory.SNAPSHOT);
            mdr.setMDEntryTypeSet(MarketDataRequest.MDENTRYTYPESET_ALL);
            mdr.setResponseFormat(IFixDefs.MSGTYPE_FXCMRESPONSE);

            UTCDate date1 = new UTCDate(request.startDate);
            UTCTimeOnly time1 = new UTCTimeOnly(request.startTime);
            UTCDate date2 = new UTCDate(request.endDate);
            UTCTimeOnly time2 = new UTCTimeOnly(request.endTime);
            UTCDate date3 = new UTCDate(date1);
            UTCTimeOnly time3 = new UTCTimeOnly(time1);
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
