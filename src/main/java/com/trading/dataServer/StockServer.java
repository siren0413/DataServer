package com.trading.dataServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;
import com.trading.ServerSocketConnectionPool.ConnectionHandler;
import com.trading.ServerSocketConnectionPool.ConnectionPool;
import com.trading.dataGenerator.domain.StockProfile;
import com.trading.dataGenerator.impl.*;

public class StockServer {

	public static void main(String[] args) {
		ConnectionPool pool = new ConnectionPool(100, 8888);
		ConnectionHandler handler = new StockConnectionHandler();
		((StockDataGenerator) handler).generate();
		pool.acceptConnections(handler);

	}

}

class StockConnectionHandler extends StockDataGenerator implements ConnectionHandler {
	private static StockProfile profile = new StockProfile();
	private static Date date = new Date();

	public void serverProcess(final Socket socket) {
		Date lastDate = new Date();
		PrintWriter writer = null;
		Set<String> symbols = new HashSet<String>();

		/**
		 * Timer task to monitor is the client was already disconnect to this
		 * server. If the remote client disconnected to this server, then close
		 * this socket.
		 */
//		final TimerTask task = new TimerTask() {
//
//			@Override
//			public void run() {
//				try {
//					socket.sendUrgentData(0);
//				} catch (Exception e) {
//					try {
//						socket.close();
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
//				}
//
//			}
//		};
		//Timer timer = new Timer();
		//timer.schedule(task, 10000, 10000);

		/**
		 * Send message to the remote client. If the remote client disconnect to
		 * this server, then the while loop breaks.
		 */
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String sym = br.readLine().trim();
			String[] syms = sym.split("\\*");
			for(String s: syms) {
				symbols.add(s);
			}
			if(symbols.size()==0)
				return;
			
			writer = new PrintWriter(socket.getOutputStream());
			while (!socket.isClosed()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(!symbols.contains(profile.getSymbol()))
					continue;
				if (lastDate.compareTo(date) != 0) {
					socket.sendUrgentData(0);
					writer.write("SYMBOL=" + profile.getSymbol() + "*NAME=" + profile.getName() + "*PRICE=" + profile.getPrice()
							+ "*TS=" + profile.getTs() + "*TYPE=" + profile.getType() + "*VOLUME=" + profile.getVolume() + "\n");
					writer.flush();
					lastDate.setTime(date.getTime());
				}
			}
		} catch (Exception e) {
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			writer.close();
			//timer.cancel();
			//timer = null;
		}

	}

	@Override
	public void getStockFeed(String name, double price, String symbol, int ts, String type, int volume) {
		profile.setName(name);
		profile.setPrice(price);
		profile.setSymbol(symbol);
		profile.setTs(ts);
		profile.setType(type);
		profile.setVolume(volume);
		date.setTime(System.currentTimeMillis());
	}

}
