package com.ollie.tierborne.client;
import com.ollie.tierborne.combat.AbilityStatus; import java.util.List;
public final class ClientAbilityState { private static List<AbilityStatus> statuses=List.of(); private ClientAbilityState(){} public static void receive(List<AbilityStatus> value){statuses=List.copyOf(value);} public static List<AbilityStatus> statuses(){return statuses;} }
