package MafiaGame;

import static MafiaGame.Mafia.roles;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Role_Mafia extends Role {
	String role = "마피아";

	public Role_Mafia(ClientHandler player) {
		super(player);
		this.player.sendToClient(new ChatMsg(ChatMsg.MODE_CONTROL,ChatMsg.CODE_ROLE,role));
	}
	
	@Override
	public String ability(String nickname) {
		return "공격 대상을 변경하고 싶으면 다시 공격하세요.";
	}
	
	@Override
	public void notifyCreation() {
		String notify=
				"\n\n***************************************************\n\n"+
				"당신은 마피아입니다.\n\n"
				+ "시민의 의심을 벗어나 조직을 승리로 이끄세요.\n\n"+
				"***************************************************\n";
		player.sendSystemMessageToClient(notify);
	}
	public String toString() {
		return role;
	}
}
