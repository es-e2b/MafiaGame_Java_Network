package MafiaGame;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

public class Mafia_Integrated extends JFrame {
	//GUI 관련 필드
	private JTextField t_input, t_userID, t_hostAddr, t_portNum, t_role;
	private static JTextArea t_players;
	JTextPane t_display;
	private JButton b_join, b_exit, b_create, b_send, b_start ,b_close;
	private DefaultStyledDocument document;

	//서버 관련 필드
	private ServerSocket serverSocket = null;
	private Thread acceptThread = null;
	static HashMap<String, ClientHandler> players = new HashMap<>();
	
	//서버-클라이언트 공통 필드
	private String serverAddress;
	private int serverPort;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	public String nickname;
	
	//클라이언트 관련 필드
	private Thread receiveThread = null;
	private Socket socket;
	private boolean isHost;
	private static PrivateChat instanceChat = null;
	private Mafia_Integrated instance;
	
	//마피아 게임 객체
	static Mafia mafia= null; //서버에서만 생성
	static void endGame() {
//		mafia.state.timer.cancel();
		mafia = null;
		broadcastingSystem("게임이 종료되었습니다.");
		broadcasting(new ChatMsg(ChatMsg.CODE_END));
		controlUpdate();
		broadcasting(new ChatMsg(ChatMsg.MODE_CONTROL, ChatMsg.CODE_ROLE, ""));
	}
	
	public Mafia_Integrated(String serverAddress, int serverPort) { // 초기 서버 주소와 포트 지정받으며 클라이언트 GUI 생성
		super("Mafia Game");
		instance=this;
		
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		
		buildGUI();
		
		setSize(900, 600);
		setLocation(400,100);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setVisible(true);
	}
	private void buildGUI() { // GUI 생성
		add(createInfoPanel(),BorderLayout.NORTH);
		add(createDisplayPanel(),BorderLayout.CENTER);
		add(createPlayerPanel(), BorderLayout.EAST);
		add(createInputPanel(), BorderLayout.SOUTH);
	}
	private JPanel createDisplayPanel() { // 대화창
		JPanel p = new JPanel(new BorderLayout());
		
		document = new DefaultStyledDocument();
		t_display = new JTextPane(document);
		
		t_display.setEditable(false);
		
		p.add(new JLabel("대화창"),BorderLayout.NORTH);
		p.add(new JScrollPane(t_display), BorderLayout.CENTER);
		
		return p;
	}
	private JPanel createPlayerPanel() { // 플레이어 목록 & 역할 필드
		JPanel p = new JPanel(new BorderLayout());
		JPanel p2 = new JPanel(new BorderLayout());
		
		t_players = new JTextArea();
		t_players.setEditable(false);
		t_role = new JTextField(15);
		t_role.setEditable(false);
		p.add(new JLabel("플레이어 목록"),BorderLayout.NORTH);
		p.add(new JScrollPane(t_players), BorderLayout.CENTER);
		p.add(p2, BorderLayout.SOUTH);
		p2.add(t_role, BorderLayout.CENTER);
		p2.add(new JLabel("  역할  "),BorderLayout.WEST);
		
		return p;
	}
	private JPanel createInfoPanel() { // 닉네임 + ip + port 입력 패널 & 서버 생성 + 입장 + 나가기 버튼 
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		t_userID = new JTextField(7);
		t_hostAddr = new JTextField(12);
		t_portNum = new JTextField(5);
		b_join = new JButton("게임 입장");
		b_join.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					serverAddress = t_hostAddr.getText();
					serverPort = Integer.parseInt(t_portNum.getText());
					try {
						connectToServer();
						sendUserID();
					}catch(UnknownHostException e1) {
						printDisplay("서버 주소와 포트 번호를 확인하세요: "+e1.getMessage());
						return;
					}catch(IOException e1) {
						printDisplay("서버와의 연결 오류: "+e1.getMessage());
						return;
					}
					setEnableExit();
					setEnableInputPanel();
					
				}
		});
		b_create = new JButton("게임 생성");
		b_create.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				serverAddress = t_hostAddr.getText();
				serverPort = Integer.parseInt(t_portNum.getText());
				acceptThread = new Thread(new Runnable() {
					public void run() {
						startServer();
					}
				});
				acceptThread.start();
				
				/*****서버 생성을 기다린 후 클라이언트로 접속하기 위한 최소 시간 슬립*********/ 
				try {
					Thread.sleep(100);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
				/*************************************************************/
				
				isHost=true;
				try {
					connectToServer();
					sendHostID();
				}catch(UnknownHostException e1) {
					printDisplay("서버 주소와 포트 번호를 확인하세요: "+e1.getMessage());
					return;
				}catch(IOException e1) {
					printDisplay("서버와의 연결 오류: "+e1.getMessage());
					return;
				}
				setEnableExit();
				setEnableInputPanel();
			}
		});
		b_exit = new JButton("게임 나가기");
		b_exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			disconnect();
			printDisplay("서버와 연결이 종료되었습니다.\n");
			setEnableJoin();
			setUnableInputPanel();
			}
		});
		t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
		t_hostAddr.setText(this.serverAddress);
		t_portNum.setText(String.valueOf(this.serverPort));
		t_portNum.setHorizontalAlignment(JTextField.CENTER);
		
		p.add(new JLabel("닉네임 "));
		p.add(t_userID);
		
		p.add(new JLabel("서버주소 "));
		p.add(t_hostAddr);
		
		p.add(new JLabel("포트 "));
		p.add(t_portNum);
		
		p.add(b_join);
		p.add(b_create);
		p.add(b_exit);
		
		b_exit.setEnabled(false);
		
		return p;
	}
	private JPanel createInputPanel() { // 채팅 입력 & 게임 시작 버튼 & GUI 종료 버튼
		JPanel p = new JPanel(new BorderLayout());
		JPanel p2 = new JPanel(new BorderLayout());
		JPanel p3 = new JPanel(new GridLayout(0,2));
		p.add(p2, BorderLayout.CENTER);
		p.add(p3, BorderLayout.EAST);
		
		t_input = new JTextField();
		t_input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		p2.add(t_input, BorderLayout.CENTER);
		b_send = new JButton("입력");
		b_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		p2.add(b_send, BorderLayout.EAST);
		
		b_start = new JButton("시작");
		b_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				if(players.size()<5) {
//					printDisplay("최소 5명부터 시작할 수 있습니다.");
//					return;
//				}
				mafia = new Mafia();
				b_start.setEnabled(false);
			}
		});
		
		b_close = new JButton("종료");
		b_close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			System.exit(0);
			}
		});
		
		b_start.setEnabled(false);
		t_input.setEnabled(false);
		b_send.setEnabled(false);
		p3.add(b_start);
		p3.add(b_close);
		
		return p;
	}
	private void printDisplay(String msg) { // 대화창에 메세지 생성
		int len = t_display.getDocument().getLength();
		try {
			document.insertString(len, msg+"\n", null);
		} catch(BadLocationException e) {
			e.printStackTrace();
		}
		
		t_display.setCaretPosition(len);
	}
	private String getLocalAddr() { // ip 읽어오기
		InetAddress local = null;
		String addr = "";
		try {
			local = InetAddress.getLocalHost();
			addr = local.getHostAddress();
			System.out.println(addr);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
		return addr;
	}
	private void connectToServer() throws UnknownHostException, IOException{ // 클라이언트 모드로 접속
		SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
		socket = new Socket();
		socket.connect(sa, 3000); // 서버가 없을 시 throws IOException
		
		out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		
		receiveThread = new Thread(new Runnable() {
			public void receiveMessages() {
			    try {
			    	ChatMsg inMsg = (ChatMsg)in.readObject();
			        if(inMsg == null) {
			        	disconnect();
			        	printDisplay("서버 연결 끊김");
			        	setEnableJoin();
				    	setUnableInputPanel();

			        	return;
			        }
			        switch(inMsg.mode) {
			        case ChatMsg.MODE_MESSAGE:
			        	printDisplay(inMsg.nickname + ": "+inMsg.message);
			        	break;
			        	
			        case ChatMsg.MODE_COMMAND:
			        	printDisplay(inMsg.nickname + ": "+inMsg.message);
			        	break;
			        case ChatMsg.MODE_SYSTEM:
			        	printDisplay(inMsg.message);
			        	break;
			        	
			        case ChatMsg.MODE_MAFIACHAT:
			        	if(instanceChat==null)
			        		instanceChat = new PrivateChat(PrivateChat.MafiaChat, instance);
			        	switch(inMsg.code) {
			        	case ChatMsg.CODE_UPDATE:
			        		privatePlayerList(inMsg.message);
			        		break;
			        	default:
			        		instanceChat.printDisplay(inMsg.nickname + ": "+inMsg.message);
			        		return;
			        	}
			        	break;
			        	
			        case ChatMsg.MODE_DEADCHAT:
			        	if(instanceChat==null) {
			        		instanceChat = new PrivateChat(PrivateChat.DeadChat, instance);
			        	}
			        	else if(instanceChat.property.equals(PrivateChat.MafiaChat)) {
			        		instanceChat.exit();
			        		instanceChat = new PrivateChat(PrivateChat.DeadChat, instance);
			        	}
			        	switch(inMsg.code) {
			        	case ChatMsg.CODE_UPDATE:
			        		privatePlayerList(inMsg.message);
			        		break;
			        	default:
			        		instanceChat.printDisplay(inMsg.nickname + ": "+inMsg.message);
			        		return;
			        	}
			        	break;
			        	
			        
			        case ChatMsg.MODE_CONTROL:
			        	/*
			        	CODE_START
			        	CODE_END
			        	CODE_DEATH
			        	CODE_DAY
			        	CODE_VOTING
			        	CODE_NIGHT
			        	 */
			        	switch(inMsg.code) {
			        	case ChatMsg.CODE_START:
			        		setUnableExit();
			        		break;
			        	case ChatMsg.CODE_END:
			        		setEnableExit();
			        		setEnableInputPanel();
			        		setEnableDisplay();
			        		if(instanceChat!=null) {
			        			instanceChat.exit();
			        			instanceChat=null;
			        		}
			        		break;
			        	case ChatMsg.CODE_DEATH:
			        		setUnableDisplay();
			        		setUnableInputPanel();
			        		setEnableExit();
			        		break;
			        	case ChatMsg.CODE_DAY:
			        		setEnableDisplay();
			        		break;
			        	case ChatMsg.CODE_VOTING:
			        		setUnableDisplay();
			        		break;
			        	case ChatMsg.CODE_NIGHT:
			        		setUnableDisplay();
			        		break;
			        	case ChatMsg.CODE_UPDATE:
			        		updatePlayerList(inMsg.message);
			        		break;
			        	case ChatMsg.CODE_KICK:
			        		setEnableJoin();
			        		setUnableInputPanel();
			        		break;
			        	case ChatMsg.CODE_ROLE:
			        		updateRole(inMsg.message);
			        		break;
			        	default:
			        		return;
			        	}
			        }
			        
			    } catch (IOException e) {
			    	//서버에서 나갔을 때
			    	printDisplay("연결을 종료했습니다.");
			    	setEnableJoin();
			    	setUnableInputPanel();
					
//					receiveThread.interrupt();
					receiveThread=null;
			    }catch(ClassNotFoundException e) {
			    	printDisplay("잘못된 객체가 전달되었습니다.");
			    }
			}
			@Override
			public void run() {
			    try {
			    	in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			    } catch (IOException e) {
			    	e.printStackTrace();
			        try {
						socket.close();
						printDisplay("서버와 연결이 종료되었습니다.\n");
						setEnableJoin();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			        return; // 스트림 열기 실패 시 스레드 종료
			    }

				while(receiveThread == Thread.currentThread()) {
					receiveMessages();
				}
			}
		});
		receiveThread.start();
	}
	private void disconnect() { // 클라이언트 모드 -> 서버 연결 해제
		send(new ChatMsg(nickname, ChatMsg.MODE_EXIT));
		t_players.setText("");
		try {
			receiveThread = null;
			socket.close();
		} catch (IOException e) {
			System.err.println("접속 끊기 오류 > "+e.getMessage());
			System.exit(-1);
		}
	}
	
	// ******************** System Control ******************************
	
	private void setUnableInputPanel() { // 입력창 & 입력 버튼 얼리기
		//게임에서 죽었을 때
		t_input.setEnabled(false);
		b_send.setEnabled(false);
	}
	private void setEnableInputPanel() { // 입력창 & 입력 버튼 녹이기
		//게임이 끝났을 때
		t_input.setEnabled(true);
		b_send.setEnabled(true);
	}
	private void setEnableExit() { // 서버 나가기 가능
		// 게임이 끝났을 때
		// 게임에서 죽었을 때
		b_join.setEnabled(false);
		b_create.setEnabled(false);
		b_exit.setEnabled(!isHost); // 호스트가 아닐 때만 서버 나가기 가능
		b_close.setEnabled(false);
		b_start.setEnabled(isHost);
		
		t_userID.setEnabled(false);
		t_hostAddr.setEnabled(false);
		t_portNum.setEnabled(false);
	}
	private void setUnableExit() { // 서버 나가기 불가
		//서버 나가기 불가 -> 게임중
		b_join.setEnabled(false);
		b_create.setEnabled(false);
		b_exit.setEnabled(false);
		b_close.setEnabled(false);
		b_start.setEnabled(false);
		
		t_userID.setEnabled(false);
		t_hostAddr.setEnabled(false);
		t_portNum.setEnabled(false);
	}
	private void setEnableJoin() { // 서버 입장/생성 가능
		//서버에서 나갔을 때
		b_join.setEnabled(true);
		b_create.setEnabled(true);
		b_exit.setEnabled(false);
		b_close.setEnabled(true);
		b_start.setEnabled(false);
		
		t_userID.setEnabled(true);
		t_hostAddr.setEnabled(true);
		t_portNum.setEnabled(true);
		
		isHost=false;
	}
	private void setUnableDisplay() {
		t_display.setEnabled(false);
	}
	private void setEnableDisplay() {
		t_display.setEnabled(true);
	}
	// ******************************************************************
	
	void send(ChatMsg msg) { // 클라이언트 모드 -> 서버에 메세지 객체 보내기
		try {
			out.writeObject(msg);
			out.flush();
		}catch(IOException e) {
			System.err.println("클라이언트 일반 전송 오류 > "+ e.getMessage());
		}
	}
	void sendMessage(){ // 클라이언트 모드 -> 서버에 일반 메세지 보내기
		String message = t_input.getText();
		if(message.isEmpty()) return;
	
		send(new ChatMsg(nickname, ChatMsg.MODE_MESSAGE, message));
		
		t_input.setText("");
	}
	void sendUserID() { // 클라이언트 모드 -> 서버에 입장 메세지 보내기 (닉네임 생성)
		nickname = t_userID.getText();
		send(new ChatMsg(nickname, ChatMsg.MODE_ENTRANCE));
	}
	void sendHostID() { // 클라이언트 모드 -> 서버에 입장 메세지 보내기 (닉네임 생성)
		nickname = t_userID.getText();
		send(new ChatMsg(nickname, ChatMsg.MODE_HOST));
	}
	static void broadcasting(ChatMsg msg) { // 서버 모드 -> 서버에 접속된 모든 클라이언트에게 메세지 객체 보내기
		for (Entry<String, ClientHandler> c : players.entrySet()) {
			c.getValue().sendToClient(msg);
		}
	}
	static void broadcasting(String msg) { // 서버 모드 -> 서버에 접속된 모든 클라이언트에게 일반 메세지 보내기
		for (Entry<String, ClientHandler> c : players.entrySet()) {
			c.getValue().sendMessageToClient(msg);
		}
	}
	static void broadcastingSystem(String msg) { // 서버 모드 -> 서버에 접속된 모든 클라이언트에게 일반 메세지 보내기
		for (Entry<String, ClientHandler> c : players.entrySet()) {
			c.getValue().sendSystemMessageToClient(msg);
		}
	}
	private void startServer() { // 서버 모드 -> 서버 생성 (서버 소켓 생성 -> 클라이언트 접속시 클라이언트 핸들러 생성)
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(serverPort);
			
			printDisplay("서버가 시작되었습니다: "+ getLocalAddr());
			while(true) {
				System.out.println("클라이언트 접속 대기..");
				clientSocket = serverSocket.accept();
				ClientHandler cHandler = new ClientHandler(clientSocket);
				cHandler.start();
				System.out.println("클라이언트 접속");
			}
			} catch (SocketException e) {
				printDisplay("서버 소켓 종료");
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				try {
					if(clientSocket != null) clientSocket.close();
					if(serverSocket != null) serverSocket.close();
					acceptThread=null;
					printDisplay("서버 생성 실패");
				}catch(IOException e) {
					System.err.println("서버 닫기 오류 > "+e.getMessage());
					System.exit(-1);
				}
		}
	}
	static void controlUpdate() {
		StringBuilder playerListBuilder=new StringBuilder();
		for(String nickname : players.keySet()) {
			if(mafia==null|| !Mafia.roles.containsKey(nickname))
				playerListBuilder.append(nickname).append("\n");
			else if(Mafia.roles.get(nickname).toString().equals("관전자"))
				playerListBuilder.append(nickname).append(" (관전자)\n");
			else if(Mafia.roles.get(nickname).toString().equals("사망"))
				playerListBuilder.append(nickname).append(" (사망)\n");
			else
				playerListBuilder.append(nickname).append(" (생존)\n");
		}
		broadcasting(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_UPDATE, playerListBuilder.toString()));
	}
	static void updatePlayerList(String playerList) {
		t_players.setText("");
		t_players.append(playerList);
	}
	private void updateRole(String role) { // 역할 출력
		t_role.setText(role);
	}

	static void privatePlayerList(String playerList) {
		instanceChat.t_players.setText("");
		instanceChat.t_players.setText(playerList);
	}
	
	public class ClientHandler extends Thread { // 서버 모드 -> 클라이언트별 통신 쓰레드 -> 클라이언트별 메세지 객체 읽기/보내기
		private Socket clientSocket;
		private ObjectOutputStream out;
		private ObjectInputStream in;
		private String nickname;
		private boolean isHost; //추후 호스트 전용 커맨드 추가시 필요
		
		public ClientHandler(Socket clientSocket){
			this.clientSocket = clientSocket;
			try {
                out = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                printDisplay("스트림 초기화 오류");
            }
		}
		private void addPlayer() {
			players.put(nickname, this);
			if(mafia!=null) {
				Mafia.deadList.add(nickname+"(관전자)");
				Mafia.roles.put(nickname, new Role_Observer(players.get(nickname)));
				Mafia.broadcastingToDead(new ChatMsg(ChatMsg.MODE_DEADCHAT, ChatMsg.CODE_UPDATE, Mafia.deadList()));
				sendToClient(new ChatMsg(ChatMsg.CODE_DEATH));
			}

			controlUpdate();
		}
		private void removePlayer() {
			players.remove(nickname);
			if(mafia!=null) {
				mafia.state.checkVictory();
				mafia.roles.remove(nickname);
			}
			controlUpdate();
		}
		private void receiveMessages(Socket cs) {
			try {
				ChatMsg msg;
				while((msg=(ChatMsg)in.readObject()) != null) {
					if(msg.mode == ChatMsg.MODE_ENTRANCE) {
						
						if(players.containsKey(msg.nickname)) {
							sendSystemMessageToClient("닉네임이 중복되었습니다.");
							return;
						}
						nickname = msg.nickname;
						addPlayer();
						broadcasting(new ChatMsg(nickname, ChatMsg.MODE_SYSTEM ,nickname+" 님이 입장하였습니다."));
//						controlUpdate();
						continue;
					}
					else if (msg.mode == ChatMsg.MODE_HOST) {
						if(players.size()==0) {
							this.isHost=true;
							printDisplay("당신은 호스트입니다.");
							nickname = msg.nickname;
							addPlayer();
							broadcasting(new ChatMsg(nickname, ChatMsg.MODE_SYSTEM ,nickname+" 님이 입장하였습니다."));
//							controlUpdate();
							continue;
						}
						sendSystemMessageToClient("이미 해당 주소로 서버가 존재합니다.");
						sendToClient(new ChatMsg(ChatMsg.CODE_KICK));
						return;
					}
					else if (msg.mode == ChatMsg.MODE_EXIT) {
						break;
					}
					if(msg.message.charAt(0)=='/') {
						String command=msg.message.substring(1);
						msg=new ChatMsg(msg.nickname,ChatMsg.MODE_COMMAND,command);
						if(isHost&&mafia==null) {
							String[] hostCommand=command.split(" ");
							int n=Integer.parseInt(hostCommand[1]);
							switch(hostCommand[0]) {
							case "낮":
								Mafia.dayTime=n;
								broadcastingSystem("낮 시간이 "+Mafia.dayTime+"초로 변경됐습니다.");
								continue;
							case "투표":
								Mafia.votingTime=n;
								broadcastingSystem("투표 시간이 "+Mafia.votingTime+"초로 변경됐습니다.");
								continue;
							case "밤":
								Mafia.nightTime=n;
								broadcastingSystem("밤 시간이 "+Mafia.nightTime+"초로 변경됐습니다.");
								continue;
							case "마피아":
								Mafia.numMafia=n;
								broadcastingSystem("마피아 수가 "+Mafia.numMafia+"명으로 변경됐습니다.");
								continue;
							case "의사":
								Mafia.numDoctor=n;
								broadcastingSystem("의사 수가 "+Mafia.numDoctor+"명으로 변경됐습니다.");
								continue;
							case "경찰":
								Mafia.numPolice=n;
								broadcastingSystem("경찰 수가 "+Mafia.numPolice+"명으로 변경됐습니다.");
								continue;
							}
						}
					}
					if(mafia!=null) {
						mafia.readMsg(msg);
						continue;
					}
					if (msg.mode == ChatMsg.MODE_MESSAGE) {
						broadcasting(msg);
					}
					else if (msg.mode == ChatMsg.MODE_COMMAND) {
						broadcasting(msg);
					}
				}
				removePlayer();
				broadcastingSystem(nickname + " 님이 퇴장하였습니다.");
			} catch (IOException e) {
				removePlayer();
				broadcastingSystem(nickname + " 님이 퇴장하였습니다.");
//				controlUpdate();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					cs.close();
				}catch (IOException e) {
					System.err.println("서버 닫기 오류 > "+e.getMessage());
					System.exit(-1);
				}
			}
		}
		
		void sendToClient(ChatMsg msg) {
			try {
				out.writeObject(msg);
				out.flush();
			}catch(IOException e) {
				System.err.println("클라이언트 일반 전송 오류 > "+e.getMessage());
			}
		}
		
		void sendMessageToClient(String msg) {
			sendToClient(new ChatMsg(nickname, ChatMsg.MODE_MESSAGE,msg));
		}
		void sendSystemMessageToClient(String msg) {
			sendToClient(new ChatMsg(ChatMsg.MODE_SYSTEM, msg));
		}
		@Override
		public void run() {
				receiveMessages(clientSocket);
		}
	}
	public static void main(String[] args) { // serverAddress = "localhost", serverPort = 54321
		
		String serverAddress = "localhost";
		int serverPort = 54321;
		
		new Mafia_Integrated(serverAddress, serverPort);
//		instanceChat = new PrivateChat(PrivateChat.DeadChat, instance);
	}
}
