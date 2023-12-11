package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Civilian extends Role {
	String role = "시민";
	public Role_Civilian(ClientHandler player) {
		super(player);
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}
	@Override
	public String ability(String nickname) {
		String notify=nickname+"의 공격을 받는 꿈을 꾸었습니다.";
		return notify;
	}
	@Override
	public void notifyCreation() {
		String notify=
				"\n\n***************************************************\n\n"+
				"당신은 시민입니다.\n\n"
				+ "마피아의 공격으로부터 살아남아 모든 마피아를 색출하세요.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);
	}
	public String toString() {
		return role;
	}
}
