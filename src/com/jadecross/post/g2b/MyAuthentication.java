package com.jadecross.post.g2b;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MyAuthentication extends Authenticator {
    PasswordAuthentication pa;

    public MyAuthentication() {
        String id = "info@jadecross.com";  // 제이드크로스 나라메일 전용 ID
        String pw = "info0001";
//		String pw = "itwtpublndcjvlzk";



        this.pa = new PasswordAuthentication(id, pw);
    }

    public PasswordAuthentication getPasswordAuthentication() {
        return this.pa;
    }
}