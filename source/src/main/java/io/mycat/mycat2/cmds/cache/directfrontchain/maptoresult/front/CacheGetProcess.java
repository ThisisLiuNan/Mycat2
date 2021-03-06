package io.mycat.mycat2.cmds.cache.directfrontchain.maptoresult.front;

import java.nio.channels.SelectionKey;

import io.mycat.mycat2.MycatSession;
import io.mycat.mycat2.cmds.cache.mapcache.CacheManager;
import io.mycat.mycat2.common.ChainExecInf;
import io.mycat.mycat2.common.SeqContextList;
import io.mycat.mycat2.console.SessionKeyEnum;
import io.mycat.proxy.ProxyBuffer;

/**
 * 缓存的获取操作
 * 
 * @since 2017年9月18日 下午4:15:50
 * @version 0.0.1
 * @author liujun
 */
public class CacheGetProcess implements ChainExecInf {

	/**
	 * 实例对象
	 */
	public static final ChainExecInf INSTANCE = new CacheGetProcess();

	@Override
	public boolean invoke(SeqContextList seqList) throws Exception {

		MycatSession session = (MycatSession) seqList.getSession();

		// 首先检查当前否为获取数据标识
		// 首先检查当前是否存在从缓存中获取数据的标识
		if (session.getSessionAttrMap().containsKey(SessionKeyEnum.SESSION_KEY_CACHE_GET_FLAG.getKey())) {

			ProxyBuffer curBuffer = session.proxyBuffer;
			long offset = 0;
			if (session.getSessionAttrMap().containsKey(SessionKeyEnum.SESSION_KEY_GET_OFFSET_FLAG.getKey())) {
				offset = (long) session.getSessionAttrMap().get(SessionKeyEnum.SESSION_KEY_GET_OFFSET_FLAG.getKey());
			}

			String sql = (String) session.getSessionAttrMap().get(SessionKeyEnum.SESSION_KEY_CACHE_SQL_STR.getKey());

			// 从缓存中获取数据
			offset = CacheManager.INSTANCE.getCacheValue(curBuffer, sql, offset);

			// 将当前的偏移放入到会话中
			session.getSessionAttrMap().put(SessionKeyEnum.SESSION_KEY_GET_OFFSET_FLAG.getKey(), offset);

			// 获取到数据后
			return seqList.nextExec();
		}
		// 当不存在标识时，直接将SQL写入数据库请求
		else {
			// 将查询的数据写入至mysql
			ProxyBuffer curBuffer = session.proxyBuffer;
			// 切换 buffer 读写状态
			curBuffer.flip();
			// 没有读取,直接透传时,需要指定 透传的数据 截止位置
			curBuffer.readIndex = curBuffer.writeIndex;
			// 改变 owner，对端Session获取，并且感兴趣写事件
			session.giveupOwner(SelectionKey.OP_WRITE);
			// 后数进行写入
			session.curBackend.writeToChannel();
		}

		return false;
	}

}
