package com.ollie.tierborne.client;
import java.util.*;
public final class ClientCloakState {private static final Set<Integer> CLOAKED=new HashSet<>();private ClientCloakState(){}public static void receive(int id,boolean active){if(active)CLOAKED.add(id);else CLOAKED.remove(id);}public static boolean isCloaked(int id){return CLOAKED.contains(id);}public static void clear(){CLOAKED.clear();}}
