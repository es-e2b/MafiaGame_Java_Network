package MafiaGame;
import java.util.*;
import java.util.Map.Entry;

import MafiaGame.Mafia_Integrated.ClientHandler;

public class Mafia {
	static HashMap<String, Role> roles;
	static GameState state;
	static List<String> mafiaList;
	static List<String> deadList;
	
	static int dayTime=60;
	static int votingTime=30;
	static int nightTime=60;
	
	static int numMafia=2;
	static int numPolice=1;
	static int numDoctor=1;
	
	public Mafia() {
		roles = new HashMap<>();
		mafiaList=new ArrayList<>();
		deadList=new ArrayList<>();
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
		else if (msg.mode==ChatMsg.MODE_MAFIACHAT) {
			broadcastingToMafia(msg);
		}
		else if (msg.mode==ChatMsg.MODE_DEADCHAT) {
			broadcastingToDead(msg);
		}
	}
	void generateRandomRole() {
        List<String> roleList = new ArrayList<>();

        int numPlayers = Mafia_Integrated.players.size();
        
        for(int i=0;i<numMafia;i++) {
        	roleList.add("마피아");
        }

        for(int i=0;i<numDoctor;i++) {
        	roleList.add("의사");
        }

        for(int i=0;i<numPolice;i++) {
        	roleList.add("경찰");
        }

        // 나머지 시민 추가 (총 플레이어 수 - 마피아 수 - 경찰 수 - 의사 수)
        int numCivilians = numPlayers - roleList.size();
        for (int i = 0; i < numCivilians; i++) {
            roleList.add("시민");
        }

        // 역할 리스트를 섞음
        Collections.shuffle(roleList);

        // 각 플레이어에게 역할을 배정
        int index = 0;
        for (String playerName : Mafia_Integrated.players.keySet()) {
            String role = roleList.get(index);
            roles.put(playerName, createRole(playerName, role));
            index++;
        }
        
        Mafia.broadcastingToMafia(new ChatMsg(ChatMsg.MODE_MAFIACHAT, ChatMsg.CODE_UPDATE, Mafia.mafiaList()));
    }

    // 역할에 따라 Role 객체를 생성하는 메서드
    private Role createRole(String playerName, String role) {
        switch (role) {
            case "마피아":
            	mafiaList.add(playerName);
                return new Role_Mafia(Mafia_Integrated.players.get(playerName));
            case "경찰":
                return new Role_Police(Mafia_Integrated.players.get(playerName));
            case "의사":
                return new Role_Doctor(Mafia_Integrated.players.get(playerName));
            default:
                return new Role_Civilian(Mafia_Integrated.players.get(playerName));
        }
    }
	static void broadcastingToAlive(ChatMsg msg) { // 마피아 게임 내의 생존자에게만 메세지 전송 -> GUI 조작 객체 전송시 활용
		for (Entry<String, ClientHandler> c : Mafia_Integrated.players.entrySet()) {
			if(!roles.get(c.getKey()).toString().equals("사망"))
				c.getValue().sendToClient(msg);
		}
	}
	static void broadcastingToMafia(ChatMsg msg) {
		for (Entry<String, ClientHandler> c : Mafia_Integrated.players.entrySet()) {
			if(roles.get(c.getKey()).toString().equals("마피아"))
				c.getValue().sendToClient(msg);
		}
	}
	static void broadcastingToDead(ChatMsg msg) {
		for (Entry<String, ClientHandler> c : Mafia_Integrated.players.entrySet()) {
			if(roles.get(c.getKey()).toString().equals("사망")
					||roles.get(c.getKey()).toString().equals("관전자")) {
				c.getValue().sendToClient(msg);
			}
		}
	}
	static String deadList() {
		StringBuilder playerListBuilder=new StringBuilder();
		for(String nickname : deadList) {
			playerListBuilder.append(nickname).append("\n");
		}
		return playerListBuilder.toString();
	}
	static String mafiaList() {
		StringBuilder playerListBuilder=new StringBuilder();
		for(String nickname : mafiaList) {
			playerListBuilder.append(nickname).append("\n");
		}
		return playerListBuilder.toString();
	}
}
