package com.jadecross.post.g2b;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.jadecross.util.DateUtil;

/**
 2021년 12월에 g2b 사이트에서 http --> https 로 변경됨에 따라 인증서 작업이 필요함
 ============ 인증서 작업 ============
 java -cp . InstallCert www.g2b.go.kr:8101
 ##### www.g2b.go.kr-2
 keytool -exportcert -keystore jssecacerts -storepass changeit -file output.cert -alias www.g2b.go.kr-2
 keytool -importcert -keystore ..\jre\lib\security\cacerts -storepass changeit -file output.cert -alias letsencrypt
 * @author Administrator
 *
 */
public class JadeSendMail {
    public static void main(String[] args) throws ClientProtocolException, IOException {
        if (args.length != 4) {
            System.out.println("아규먼트가 부족합니다. 아규먼트의 갯수는 4개 이상 ");
            System.out.println("Usage: ");
            System.out.println("java JadeSendMail \"성능,a,b\" \"emaillist\" \"한달or오늘or어제\" \"품목명or첨부포함\"");
            System.exit(0);
        }
        JadeSendMail smt = new JadeSendMail();

        StringTokenizer st = new StringTokenizer(args[0], ",");

        String url = null;
        String searchKeywords = args[0];
        String searchKeyWord = null;
        String searchTerm = args[2];
        String searchResult = "";
        String fromDate = null;
        String toDate = null;
        if (args[2].matches("한달")) {
            fromDate = DateUtil.getFromDate("yyyy/MM/dd");
            toDate = DateUtil.getToday("yyyy/MM/dd");
        } else if (args[2].matches("오늘")) {
            fromDate = DateUtil.getToday("yyyyMMdd");
            toDate = DateUtil.getToday("yyyyMMdd");
        } else if (args[2].matches("어제")) {
            fromDate = DateUtil.getFromYesterday("yyyyMMdd");
            toDate = DateUtil.getToday("yyyyMMdd");
        } else {
            System.out.println("#### 아규먼트가 잘 못 되었습니다. #### ");
            System.out.println("아래 3가지 중 1가지를 입력하세요. ");
            System.out.println("## 한달, 오늘, 어제 ##");
            System.out.println("1. 한달 : 지난 한달간 입찰 공고 목록 검색 결과");
            System.out.println("2. 오늘 : 사전 규격 메뉴에서 오늘 날짜로 검색한 결과");
            System.out.println("3. 어제 : 사전 규격 메뉴에서 어제부터 오늘까지 검색한 결과");

            System.exit(0);
        }
        String srchFd = null;
        if (args[3].matches("품목명")) {
            srchFd = "tt";
        } else if (args[3].matches("첨부포함")) {
            srchFd = "ALL";
        } else {
            srchFd = "tt";
        }

        // 메일 Header 설정

        printHead(searchKeywords, searchTerm, srchFd);

        while (st.hasMoreTokens()) {
            searchKeyWord = st.nextToken();
            try {
                if (args[2].matches("한달")) {
                    searchResult = searchResult + "검색키워드 :<font color='red'> <b>" + searchKeyWord + "</b></font>";
                    searchResult = searchResult + "<table border='3'>";

                    url = smt.makeURLMonthly(smt.urlEncode(searchKeyWord), fromDate, toDate);
                    searchResult = searchResult + smt.requestSearch(url, args[2]);
                } else {
                    url = smt.makeURLDaily(smt.urlEncode(searchKeyWord), fromDate, toDate, srchFd);
                    searchResult = searchResult + "검색키워드 :<font color='red'> <b>" + searchKeyWord + "</b></font>";

                    searchResult = searchResult + "<br><br> URL = ";
                    searchResult = searchResult + "<a href=\"" + url + "\">" + url + "</a>";
                    searchResult = searchResult + "<br><br>";

                    searchResult = searchResult + "<table border='3'>";

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpget = new HttpGet(url);

                    HttpResponse response = httpClient.execute(httpget);
                    HttpEntity entity = response.getEntity();

//					System.out.println("----------------------------------------");
                    if (entity != null) {
//						System.out.println("Response content length: " + entity.getContentLength());
                        BufferedReader rd = new BufferedReader(
                                new InputStreamReader(response.getEntity().getContent(), "euc-kr"));
                        String line = "";
                        int pageNo = 0;
                        while ((line = rd.readLine()) != null) {
                            if (line.contains("<h3 class=\"tit\">사전규격 검색 결과")) {
                                Pattern resultCnt = Pattern.compile("[0-9]{2,3}");
                                Matcher res = resultCnt.matcher(line);
                                if (res.find()) {
                                    pageNo = Integer.parseInt(res.group()) / 10 + 1;
                                }
//								System.out.println(line);
                            }
                            if (line.contains("<a href=\"javascript:showGgLinkView")) {
//								System.out.println("sjkim >> " + line);

                                String val1 = "";
                                String type = "";
                                String target = "";
                                int count = 0;
                                Pattern pInt = Pattern.compile("\\d+");
                                Pattern pHangul = Pattern.compile("([ㄱ-ㅎ가-힣]*)/([ㄱ-ㅎ가-힣]*)");
                                Matcher m1 = pInt.matcher(line);
                                Matcher m2 = pHangul.matcher(line);
                                while (m1.find()) {
                                    if (count % 2 == 0) {
                                        val1 = m1.group();
                                    } else {
                                        type = m1.group();
                                    }
                                    count++;
                                }
                                if (m2.find()) {
                                    target = smt.urlEncode(m2.group());
                                }
                                searchResult = searchResult + "<a href=\""
                                        + smt.makeLinkdoUrl(smt.urlEncode(searchKeyWord), fromDate, toDate, srchFd)
                                        + "&val1=" + val1 + "&type=" + type + "&target=" + target + "\">";

//								System.out.println("<a href=\""
//										+ smt.makeLinkdoUrl(smt.urlEncode(searchKeyWord), fromDate, toDate, srchFd)
//										+ "&val1=" + val1 + "&type=" + type + "&target=" + target + "\">");
                            } else if (line.contains("<a href=\"#self\" class=\"btn_toggle\">펼치기</a>")) {
                                searchResult = searchResult + "&nbsp; ";
                            } else if (!line.contains("<a href=\"javascript:goSort('R');\">정확도순</a></li>")) {
                                if (!line.contains("<a href=\"javascript:goSort('DD');\">접수일순</a></li>")) {
                                    if (line.contains("<a href=\"javascript:gotoPage(")) {
                                        String linkpage = "";
                                        for (int i = 1; i <= pageNo; i++) {
                                            linkpage = linkpage + "<a href=\""
                                                    + smt.makePaginationLink(smt.urlEncode(searchKeyWord), fromDate,
                                                    toDate, srchFd, i)
                                                    + "\">" + i + "</a> &nbsp;";
                                        }
                                        searchResult = searchResult + "<div class=\"pagination\"><span class=\"page\">"
                                                + linkpage + "</div>";
//										System.out.println("<div class=\"pagination\"><span class=\"page\">" + linkpage
//												+ "</div>");
                                    } else {
                                        searchResult = searchResult + line;
//										System.out.println(line);
                                    }
                                }
                            }
                        }
                    }
                    httpget.abort();

//					System.out.println("----------------------------------------");
                }
                searchResult = searchResult + "</table>";
                searchResult = searchResult + "<br>";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            System.out.println(searchResult);
            searchResult = "";
        }

        // 2023.05.27 Gmail 정책변경으로 사용불가
        //	- CentOS mailX 패키지로 대체
//		try {
//			smt.sendEmail(searchResult, args[0], args[1], args[2], args[3]);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		System.out.println("---------");

        System.out.println("</body></html>");
    }

    /**
     * 메일내용의 Head 출력
     * @param searchKeywords
     * @param searchTerm
     * @param srchFd
     */
    private static void printHead(String searchKeywords, String searchTerm, String srchFd) {
        if (searchTerm.matches("한달")) {
            System.out.println("Subject: " + "[제이드크로스] 오늘의 나라장터 검색 메일입니다.");
            System.out.println("Content-Type: text/html; charset=utf-8");
            System.out.println("From : JADECROSS <info@jadecross.com>");
            System.out.println("<html><head>\n" +
                    "  <style>\n" +
                   // "    hr { height: 3px; background-color: black;} \n" +
                    "    th {color: white; background-color: #555555; padding: 10px;}\n" +
                    "    td {padding: 10px;}\n" +
                    "    table {color: black; border-collapse:collapse;}\n" +
                    "  </style>\n" +
                    "</head><body>");
            System.out.println(" 오늘 일자로 지난 한달간 나라장터에서  <b>[" + searchKeywords + "]</b> 으로 검색한 결과입니다. " + "<br>" + "<br>");
        } else if (searchTerm.matches("오늘")) {
            System.out.println("Subject: " + "[제이드크로스] 오늘의 나라장터 사전규격형 [품목명]검색 메일입니다.");
            System.out.println("Content-Type: text/html; charset=utf-8");
            System.out.println("From : JADECROSS <info@jadecross.com>");
            System.out.println("<html><head>\n" +
                    "  <style>\n" +
                    //"    hr { height: 3px; background-color: black;} \n" +
                    //"    th {color: white; background-color: #555555; padding: 10px;}\n" +
                    "    td {padding: 10px;}\n" +
                    "    table {color: black; border-collapse:collapse;}\n" +
                    "  </style>\n" +
                    "</head><body>");
            System.out.println(" 오늘 일자로 나라장터 사전규격형 메뉴에서 검색범위를 <b>[품목명]</b>으로 지정하여  <b>[" + searchKeywords + "]</b> 으로 검색한 결과입니다. ");
        } else if (searchTerm.matches("어제")) {
            System.out.println("Subject: " + "[제이드크로스] 오늘의 나라장터 사전규격형 [첨부포함] 검색메일입니다.");
            System.out.println("Content-Type: text/html; charset=utf-8");
            System.out.println("From : JADECROSS <info@jadecross.com>");
            System.out.println("<html><head>\n" +
                    "  <style>\n" +
                    //"    hr { height: 3px; background-color: black;} \n" +
                    //"    th {color: white; background-color: #555555; padding: 10px;}\n" +
                    "    td {padding: 10px;}\n" +
                    "    table {color: black; border-collapse:collapse;}\n" +
                    "  </style>\n" +
                    "</head><body>");
            System.out.println(" 어제/오늘 날짜로 나라장터 사전규격형 메뉴에서 검색범위를 <b>[첨부포함]</b>으로 지정하여 <b>[" + searchKeywords + "]</b> 으로 검색한 결과입니다. " + "<br>" + "<br>");
        }
    }

    public String makeURLMonthly(String search, String fromDate, String toDate) {
        String url = "https://www.g2b.go.kr:8101/ep/tbid/tbidList.do?taskClCds=&bidNm=" + search + "&searchDtType=1"
                + "&fromBidDt=" + fromDate + "&toBidDt=" + toDate
                + "&fromOpenBidDt=&toOpenBidDt=&radOrgan=1&instNm=&area=&regYn=Y&bidSearchType=1&searchType=1";

//		System.out.println("##################################");
//		System.out.println(url);
//		System.out.println("##################################");

        return url;
    }

    public String makeURLDaily(String search, String fromDate, String toDate, String srchFd) {
        String url = "https://www.g2b.go.kr:8340/body.do?&kwd=" + search + "&category=GG" + "&subCategory=ALL"
                + "&detailSearch=true" + "&sort=R" + "&reSrchFlag=false" + "&pageNum=1" + "&srchFd=" + srchFd + "&date="
                + "&startDate=" + fromDate + "&endDate=" + toDate + "&startDate2=" + "&endDate2=" + "&orgType=balju"
                + "&orgName=" + "&orgCode=" + "&swFlag=%C0%FC%C3%BC" + "&dateType=" + "&area=" + "&gonggoNo="
                + "&preKwd=" + "&preKwds=" + "&body=yes";

        url = url.trim();
//		System.out.println("##################################");
//		System.out.println(url);
//		System.out.println("##################################");

        return url;
    }

    public String makeLinkdoUrl(String search, String fromDate, String toDate, String srchFd) {
        String url = "https://www.g2b.go.kr:8340/link.do?&kwd=" + search + "&category=GG" + "&subCategory=ALL"
                + "&detailSearch=true" + "&sort=R" + "&reSrchFlag=false" + "&pageNum=1" + "&srchFd=" + srchFd + "&date="
                + "&startDate=" + fromDate + "&endDate=" + toDate + "&orgType=balju" + "&orgName=" + "&orgCode="
                + "&swFlag=%C0%FC%C3%BC" + "&dateType=" + "&area=" + "&gonggoNo=";
        url = url.trim();

        return url;
    }

    public String makePaginationLink(String search, String fromDate, String toDate, String srchFd, int pageNum) {
        String url = "https://www.g2b.go.kr:8340/body.do?&kwd=" + search + "&category=GG" + "&subCategory=ALL"
                + "&detailSearch=true" + "&sort=R" + "&reSrchFlag=false" + "&pageNum=" + pageNum + "&srchFd=" + srchFd
                + "&date=" + "&startDate=" + fromDate + "&endDate=" + toDate + "&startDate2=" + "&endDate2="
                + "&orgType=balju" + "&orgName=" + "&orgCode=" + "&swFlag=%C0%FC%C3%BC" + "&dateType=" + "&area="
                + "&gonggoNo=" + "&preKwd=" + "&preKwds=" + "&body=yes";
        url = url.trim();

        return url;
    }

    public String urlEncode(String search) throws UnsupportedEncodingException {
        String urlEncoding = URLEncoder.encode(search, "euc-kr");

        return urlEncoding;
    }

    public String requestSearch(String url, final String period) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        String content = "";
        try {
            content = (String) httpClient.execute(httpget, new BasicResponseHandler() {
                public String handleResponse(HttpResponse response) throws HttpResponseException, IOException {
                    String res = new String(super.handleResponse(response).getBytes("euc-kr"), "euc-kr");
                    Document doc = Jsoup.parse(res);
                    if (period.matches("한달")) {
                        Elements title = doc.select("th");
                        Elements rows = doc.select("tbody");

//						System.out.println(title.toString());
//						System.out.println(rows.toString());

                        return "<br>" + title.toString() + rows.toString() + "<br>";
                    }
                    Elements contents = doc.select("html");

                    return "<br> " + contents.toString() + "<br>";
                }
            });
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public boolean sendEmail(String content, String searchKeywords, String recipients, String term, String srchFd)
            throws IOException {
        Properties p = new Properties();
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.host", "smtp.gmail.com");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.port", "587");

        Authenticator auth = new MyAuthentication();

        Session session = Session.getDefaultInstance(p, auth);
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setSentDate(new Date());

            InternetAddress from = new InternetAddress();

            from = new InternetAddress("JADECROSS<info@jadecross.com>");
            StringTokenizer stRecipiants = new StringTokenizer(recipients, ",");
            while (stRecipiants.hasMoreTokens()) {
                msg.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(stRecipiants.nextToken() + "@jadecross.com"));
            }
            msg.setFrom(from);
            if (term.matches("한달")) {
                msg.setSubject("[제이드크로스] 오늘의 나라장터 검색메일입니다.", "UTF-8");
                msg.setText(" 오늘 일자로 지난 한달간 나라장터에서  <b>[" + searchKeywords + "]</b> 으로 검색한 결과입니다. " + "<br>" + "<br>"
                        + content, "UTF-8");
            } else if (term.matches("오늘")) {
                msg.setSubject("[제이드크로스] 오늘의 나라장터 사전규격형 [" + srchFd + "]검색메일입니다.", "UTF-8");
                msg.setText(" 오늘 일자로 나라장터 사전규격형 메뉴에서 검색범위를 <b>[" + srchFd + "]</b>으로 지정하여  <b>[" + searchKeywords
                        + "]</b> 으로 검색한 결과입니다. " + "<br>" + "<br>" + content, "UTF-8");
            } else if (term.matches("어제")) {
                msg.setSubject("[제이드크로스] 오늘의 나라장터 사전규격형 [" + srchFd + "] 검색메일입니다.", "UTF-8");
                msg.setText(" 어제/오늘 날짜로 나라장터 사전규격형 메뉴에서 검색범위를 <b>[" + srchFd + "]</b>으로 지정하여 <b>[" + searchKeywords
                        + "]</b> 으로 검색한 결과입니다. " + "<br>" + "<br>" + content, "UTF-8");
            }
            msg.setHeader("content-Type", "text/html");

            Transport.send(msg);
        } catch (AddressException addr_e) {
            addr_e.printStackTrace();
        } catch (MessagingException msg_e) {
            msg_e.printStackTrace();
        }
        return true;
    }
}