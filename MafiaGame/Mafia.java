package MafiaGame;
import java.util.HashMap;
import java.util.Map.Entry;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Mafia {
	static HashMap<String, Role> roles = new HashMap<>();
	static GameState state;
	public Mafia() {
		Mafia_Integrated.broadcastingSystem("마피아 게임이 시작되었습니다.");
		Mafia_Integrated.broadcasting(new ChatMsg(ChatMsg.CODE_START));
		generateRandomRole();
		state=new GameState_Day();
		Mafia_Integrated.controlUpdate();
	}
	void readMsg(ChatMsg msg) {
		if(msg.mode==ChatMsg.MODE_MESSAGE) {
			if(roles.get(msg.nickname).toString().equals("사망") || roles.get(msg.nickname).toString().equals("관전자"))
				Mafia_Integrated.players.get(msg.nickname).sendSystemMessageToClient("생존자만 대화할 수 있습니다.");
			else
				state.readMessage(msg);
		}
		else if(msg.mode==ChatMsg.MODE_COMMAND) {
			String returnMessage = state.resultAbility(msg.nickname ,msg.message);
			Mafia_Integrated.players.get(msg.nickname).sendSystemMessageToClient(returnMessage);
		}
	}
	void generateRandomRole() {
		for(Entry<String, ClientHandler> c : Mafia_Integrated.players.entrySet())
			roles.put(c.getKey(), new Role_Civilian(c.getValue()));
	}
	static void broadcastingToAlive(ChatMsg msg) { // 마피아 게임 내의 생존자에게만 메세지 전송 -> GUI 조작 객체 전송시 활용
		for (Entry<String, ClientHandler> c : Mafia_Integrated.players.entrySet()) {
			if(roles.get(c.getKey()).toString()!="사망")
				c.getValue().sendToClient(msg);
		}
	}
}
