package com.transys.controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.transys.util.OpcDataMap;

@Controller
public class MainController {
	
	//오토닉스 서버 IP
	public static final String MAIN_IP = "192.168.1.63";
	public static final String SUB_IP = "192.168.1.63";
	public static final int PORT = 5660;
	
	public static OpcUaClient client = null;
	
	public static OpcUaClient clientMain = null;
	public static OpcUaClient clientSub = null;

	//설비별 출고가능신호
	public static boolean outPutChk1 = false;
	public static boolean outPutChk2 = false;
	public static boolean outPutChk3 = false;
	public static boolean outPutChk4 = false;
	public static boolean outPutChk5 = false;
	public static boolean outPutChk6 = false;
	public static boolean outPutChk7 = false;

	//창고 입고카운트
	public static int plcCount = 0;
	
	//2025-03-20 추가
	//MSSQL 기존서버 IP(OCTOSYS)
	public static final String MSSQL_OCTO_IP = "192.168.1.59";
//	public static final String MSSQL_OCTO_IP = "192.168.1.61";
	//MSSQL 신규서버 IP(EZ)
	public static final String MSSQL_EZ_IP = "192.168.1.59";
//	public static final String MSSQL_EZ_IP = "192.168.1.62";
	//false : OCTOSYS, true : EZ
	public static boolean mssqlSearchChk = false;
	public static boolean mssqlOCTOChk = false;
	public static boolean mssqlEZChk = false;
	
	//DB체크 
	public static void dbCheck() {
    	
		//2025-03-20 추가
		mssqlOCTOChk = pingTest(MSSQL_OCTO_IP);
		mssqlEZChk = pingTest(MSSQL_EZ_IP);
/*
		System.out.println("mssqlOCTOChk : "+mssqlOCTOChk);
		System.out.println("mssqlEZChk : "+mssqlEZChk);
*/
		//OCTOSYS서버 PING 연결되면 연결
		//안되면 EZ서버 연결
		
		if(mssqlEZChk) {
			mssqlSearchChk = true;
		}

		if(mssqlOCTOChk) {
			mssqlSearchChk = false;
		}
				
	}
	//통신상태 체크
	public static void commCheck() {
		
    	//1.메인, 백업서버의 핑테스트
    	boolean mainPingCheck = pingTest(MAIN_IP);
    	boolean subPingCheck = pingTest(SUB_IP);
    	
    	
//    	System.out.println("메인아이피 : "+mainPingCheck);
//    	System.out.println("서브아이피 : "+subPingCheck);
    	
    	//2.메인, 백업서버의 포트가 열려있는지 닫혀있는지 테스트
    	boolean mainPortCheck = portTest(MAIN_IP, PORT);
    	boolean subPortCheck = portTest(SUB_IP, PORT);
    	
//    	System.out.println("메인포트 : "+mainPortCheck);
//    	System.out.println("서브포트 : "+subPortCheck);
    	String tagValueSub = "-1";
    	String tagValueMain = "-1";

		if(mainPingCheck && mainPortCheck) {

			if(clientMain != null) {
				//IP : OK, PORT : OK, OPC : OK
				//값을 하나 받아와서 해당값이 0이 아니라면 연결유지, 0이라면 null로 변경
				OpcDataMap opcDataMap = new OpcDataMap();

				try {
					Map<String, Object> dataMap = opcDataMap.getOpcDataConnCheck(clientMain, "Transys.$SYSTEM$.$SystemStatus$");
					tagValueMain = dataMap.get("value").toString();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
//				System.out.println("tagValueMain : "+tagValueMain);


			}else {
				clientMain = opcConnection(MAIN_IP, PORT);				
			}

		}else {
			clientMain = null;
		}
		
		if(subPingCheck && subPortCheck) {

			if(clientSub != null) {
				//IP : OK, PORT : OK, OPC : OK
				//값을 하나 받아와서 해당값이 0이 아니라면 연결유지, 0이라면 null로 변경
				OpcDataMap opcDataMap = new OpcDataMap();
				
				try {
					Map<String, Object> dataMap = opcDataMap.getOpcDataConnCheck(clientSub, "Transys.$SYSTEM$.$SystemStatus$");
					tagValueSub = dataMap.get("value").toString();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
//				System.out.println("tagValueSub : "+tagValueSub);
				
			}else {
				clientSub = opcConnection(SUB_IP, PORT);				
			}

		}else {
			clientSub = null;
		}
		
		if("ACTIVE".equals(tagValueMain)) {
			client = clientMain;
		}
		
		if("ACTIVE".equals(tagValueSub)) {
			client = clientSub;
		}
    	
	}
	
    //OPC서버 연결시작
    public static void opcStart() {
    	
    	try {
			client.connect().get();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}
    }    
    
    //OPC서버 연결종료
    public static void opcEnd() {
		try {
			client.disconnect().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
    }	
    
    @RequestMapping(value= "/", method = RequestMethod.GET)
    public String ccf01_02(Model model) {
        return "/ccf/ccf01_02.jsp"; // 
    }
    
    //메인, 백업서버 핑테스트 메서드
    public static boolean pingTest(String ip) {
    	boolean statusCheck = false;
    	try {
			InetAddress pingCheck = InetAddress.getByName(ip);
			
			
			if(pingCheck.isReachable(200)) {
				statusCheck = true;
			}else {
				statusCheck = false;
			}
		
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return statusCheck;
    }
    
    //메인, 백업 포트 열려있는지, 닫혀있는지 확인
	public static boolean portTest(String ip, int port) {
    	boolean portCheck = false;
    	
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 500);
            portCheck = true; // 연결 성공
        }catch (IOException e) {
//			e.printStackTrace();
		}
    	
    	return portCheck;
    }
	
	public static void portEnd(String ip, int port) {
		Socket socket;
		try {
			socket = (new Socket(ip, port));
			socket.close();	
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//OPC 커넥션 생성
	public static OpcUaClient opcConnection(String ip, int port) {
		OpcUaClient opcUaClient = null;
		
		try {
			opcUaClient = OpcUaClient.create("opc.tcp://"+ip+":"+port);
			opcUaClient.connect().get();
			
		} catch (UaException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return opcUaClient; 
	}
	
	//OPC커넥션 해제
	public static void opcDisConnection(OpcUaClient connClient) {
		try {
			connClient.disconnect().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
