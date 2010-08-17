/**
 *   Copyright 2010 Peter Klauser
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
*/
package com.googlecode.protobuf.pro.duplex.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;

import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClient;
import com.googlecode.protobuf.pro.duplex.RpcServiceRegistry;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.listener.TcpConnectionEventListener;
import com.googlecode.protobuf.pro.duplex.logging.CategoryPerServiceLogger;
import com.googlecode.protobuf.pro.duplex.logging.RpcLogger;

public class DuplexTcpServerBootstrap extends ServerBootstrap {

	private static Log log = LogFactory.getLog(DuplexTcpServerBootstrap.class);
	
	private List<TcpConnectionEventListener> connectionEventListeners = new ArrayList<TcpConnectionEventListener>();
	
	private final PeerInfo serverInfo;
	private final RpcServiceRegistry rpcServiceRegistry = new RpcServiceRegistry();
	private final RpcClientRegistry rpcClientRegistry = new RpcClientRegistry();
	
	public DuplexTcpServerBootstrap(PeerInfo serverInfo, ChannelFactory channelFactory, RpcServerCallExecutor rpcServerCallExecutor) {
		this(serverInfo, channelFactory, rpcServerCallExecutor, new CategoryPerServiceLogger());
	}
	
	public DuplexTcpServerBootstrap(PeerInfo serverInfo, ChannelFactory channelFactory, RpcServerCallExecutor rpcServerCallExecutor, RpcLogger logger) {
		super(channelFactory);
		if ( serverInfo == null ) {
			throw new IllegalArgumentException("serverInfo");
		}
		if ( rpcServerCallExecutor == null ) {
			throw new IllegalArgumentException("rpcServerCallExecutor");
		}
		this.serverInfo = serverInfo;

		TcpConnectionEventListener informer = new TcpConnectionEventListener(){
			@Override
			public void connectionClosed(RpcClient client) {
				for( TcpConnectionEventListener listener : getListenersCopy() ) {
					listener.connectionClosed(client);
				}
			}
			@Override
			public void connectionOpened(RpcClient client) {
				for( TcpConnectionEventListener listener : getListenersCopy() ) {
					listener.connectionOpened(client);
				}
			}
		};
    	
		DuplexTcpServerPipelineFactory sf = new DuplexTcpServerPipelineFactory(serverInfo, rpcServiceRegistry, rpcClientRegistry, rpcServerCallExecutor, informer, logger); 
		setPipelineFactory(sf);
	}

	@Override
	public Channel bind( SocketAddress localAddress ) {
		if ( localAddress == null ) {
			throw new IllegalArgumentException("localAddress");
		}
		if ( localAddress instanceof InetSocketAddress ) {
			if ( serverInfo.getPort() != ((InetSocketAddress) localAddress).getPort() ) {
				log.warn("localAddress " + localAddress + " does not match serverInfo's port " + serverInfo.getPort());
			}
		}
		return super.bind(localAddress);
	}
	
	@Override
	public Channel bind() {
    	return bind( new InetSocketAddress(serverInfo.getPort()));
	}
	
	private List<TcpConnectionEventListener> getListenersCopy() {
		List<TcpConnectionEventListener> copy = new ArrayList<TcpConnectionEventListener>();
		copy.addAll(getConnectionEventListeners());
		
		return Collections.unmodifiableList(copy);
	}
	
	public void registerConnectionEventListener( TcpConnectionEventListener listener ) {
		getConnectionEventListeners().add(listener);
	}
	
	public void removeConnectionEventListener( TcpConnectionEventListener listener ) {
		getConnectionEventListeners().remove(listener);
	}
	
	/**
	 * @return the connectionEventListeners
	 */
	public List<TcpConnectionEventListener> getConnectionEventListeners() {
		if ( connectionEventListeners == null ) {
			return new ArrayList<TcpConnectionEventListener>(0);
		}
		return connectionEventListeners;
	}

	/**
	 * @param connectionEventListeners the connectionEventListeners to set
	 */
	public void setConnectionEventListeners(
			List<TcpConnectionEventListener> connectionEventListeners) {
		this.connectionEventListeners = connectionEventListeners;
	}

	/**
	 * @return the rpcServiceRegistry
	 */
	public RpcServiceRegistry getRpcServiceRegistry() {
		return rpcServiceRegistry;
	}

	/**
	 * @return the rpcClientRegistry
	 */
	public RpcClientRegistry getRpcClientRegistry() {
		return rpcClientRegistry;
	}

	/**
	 * @return the serverInfo
	 */
	public PeerInfo getServerInfo() {
		return serverInfo;
	}

}
