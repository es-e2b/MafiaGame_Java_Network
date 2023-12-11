package MafiaGame;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class PrivateChat extends JFrame{
	public final static String MafiaChat = "MafiaChat";
	public final static String DeadChat = "DeadChat";
	public String property;
	private final Mafia_Integrated instance;
	
	public JTextField t_input;
	public JTextArea t_players;
	public JTextPane t_display;
	private JButton b_send;
	private DefaultStyledDocument document;
	
	public PrivateChat(String property, Mafia_Integrated instance) {
		super(property);
		this.property=property;
		this.instance=instance;
		buildGUI();
		
		setSize(600, 300);
		setLocation(600,900);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setVisible(true);
	}
	private void buildGUI() { // GUI 생성
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
		p.add(new JLabel("플레이어 목록"),BorderLayout.NORTH);
		p.add(new JScrollPane(t_players), BorderLayout.CENTER);
		p.add(p2, BorderLayout.SOUTH);
		
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
		
		t_input.setEnabled(true);
		b_send.setEnabled(true);
		
		return p;
	}
	void sendMessage(){ // 클라이언트 모드 -> 서버에 일반 메세지 보내기
		String message = t_input.getText();
		if(message.isEmpty()) return;
	
		if(property.equals(MafiaChat))
			instance.send(new ChatMsg(instance.nickname, ChatMsg.MODE_MAFIACHAT, message));
		else
			instance.send(new ChatMsg(instance.nickname, ChatMsg.MODE_DEADCHAT, message));
		
		t_input.setText("");
	}
	public void printDisplay(String msg) { // 대화창에 메세지 생성
		int len = t_display.getDocument().getLength();
		try {
			document.insertString(len, msg+"\n", null);
		} catch(BadLocationException e) {
			e.printStackTrace();
		}
		
		t_display.setCaretPosition(len);
	}
	public void exit() {
		this.dispose();
	}
}
