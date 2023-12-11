package MafiaGame;

import java.io.Serializable;

import javax.swing.ImageIcon;

public class ChatMsg implements Serializable {
	public final static int MODE_ENTRANCE = 0x1;
	public final static int MODE_HOST = 0x2;
	public final static int MODE_EXIT = 0x3;
	public final static int MODE_MESSAGE = 0x4; // nickname : 메세지 전달
	public final static int MODE_COMMAND = 0x8; // '/'명령어를 통해 시스템에만 전달
	public final static int MODE_SYSTEM = 0x10; // nickname 없이 전달
	public final static int MODE_MAFIACHAT = 0x20;
	public final static int MODE_DEADCHAT = 0x30;
	public final static int MODE_CONTROL = 0x40;
	public final static int CODE_START = 0x80;
	public final static int CODE_END = 0x100;
	public final static int CODE_DEATH = 0x200;
	public final static int CODE_DAY = 0x400;
	public final static int CODE_VOTING = 0x800;
	public final static int CODE_NIGHT = 0x1000;
	public final static int CODE_UPDATE = 0x2000;
	public final static int CODE_KICK = 0x4000;
	public final static int CODE_ROLE = 0x8000;

	String nickname;
	int mode;
	String message;
	int code;

	public ChatMsg(String nickname, int mode, String message, int code){
		this.nickname = nickname;
		this.mode=mode;
		this.message=message;
		this.code=code;
	}
	public ChatMsg(String nickname, int mode, String message) { // 일반 메세지 && 커맨드 && 전용 채팅 전송
		this(nickname, mode, message, 0);
	}
	public ChatMsg(String nickname, int mode) { // 유저 입장 & 퇴장
		this(nickname, mode, "", 0);
	}
	public ChatMsg(int mode, String message) { // 시스템 메세지 전송
		this("", mode, message, 0);
	}
	public ChatMsg(int mode, int code,String message) { // UPDATE용 생성자
		this("", mode, message, code);
	}
	public ChatMsg(int code) { // GUI 컨트롤 메세지 전송
		this("", MODE_CONTROL, "", code);
	}
}
