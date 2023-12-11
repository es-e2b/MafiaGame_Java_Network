package MafiaGame;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Doctor extends Role {
	String role = "의사";

	public Role_Doctor(ClientHandler player) {
		super(player);
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}

	@Override
	public String ability(String nickname) {
		return "[" + nickname + "]" + "님을 치료합니다.\n"
				+ "치료 대상을 변경하고 싶으면 다시 지목하세요.";
	}

	@Override
	public void notifyCreation() {
		String notify=
				"\n\n***************************************************\n\n"+
				"당신은 의사입니다.\n\n"
				+ "마피아의 공격으로부터 시민을 보호하세요.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);

	}
	public String toString() {
		return role;
	}

}
