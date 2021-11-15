# mysql

![1636993679(1)](D:\study\github\study_lagou\笔记\第四阶段\mysqlMd\1636993679(1).jpg)

##  InnoDB架构

#### InnoDB和MyISAM区别:

- 事务和外键

  InnoDB支持事务和外键，具有安全性和完整性，适合大量insert或update操作

  MyISAM不支持事务和外键，它提供高速存储和检索，适合大量的select查询操作

- 锁机制

  InnoDB支持行级锁，锁定指定记录。基于索引来加锁实现。

  MyISAM支持表级锁，锁定整张表。

- 索引结构

  InnoDB使用聚集索引（聚簇索引），索引和记录在一起存储，既缓存索引，也缓存记录。

  MyISAM使用非聚集索引（非聚簇索引），索引和记录分开。

- 并发处理能力

  MyISAM使用表锁，会导致写操作并发率低，读之间并不阻塞，读写阻塞。

  InnoDB读写阻塞可以与隔离级别有关，可以采用多版本并发控制（MVCC）来支持高并发

- 存储文件

  InnoDB表对应两个文件，一个.frm表结构文件，一个.ibd数据文件。InnoDB表最大支持64TB；

  MyISAM表对应三个文件，一个.frm表结构文件，一个MYD表数据文件，一个.MYI索引文件。从MySQL5.0开始默认限制是256TB。  

#### mysql 8.0架构图:

![image-20211115230713670](D:\study\github\study_lagou\笔记\第四阶段\mysqlMd\image-20211115230713670.png)

##### 内存结构:

- buffer pool: 缓冲池,BP以Page页为单位，默认大小16K，BP的底层采用链表数据结构管理Page。在InnoDB访问表记录和索引时会在Page页中缓存，以后使用可以减少磁盘IO操作，提升效率。  

  Page管理机制
  Page根据状态可以分为三种类型：
  	free page ： 空闲page，未被使用
  	clean page：被使用page，数据没有被修改过
  	dirty page：脏页，被使用page，数据被修改过，页中数据和磁盘的数据产生了不一致
  针对上述三种page类型，InnoDB通过三种链表结构来维护和管理
  	free list ：表示空闲缓冲区，管理free page
  	flush list：表示需要刷新到磁盘的缓冲区，管理dirty page，内部page按修改时间排序。脏页即存在于flush链表，也在LRU链表中，但是两种互不影响，LRU链表负责管理page的可用性和释放，而flush链表负责管理脏页的刷盘操作。
  	lru list：表示正在使用的缓冲区，管理clean page和dirty page，缓冲区以midpoint为基点，前面链表称为new列表区，存放经常访问的数据，占63%；后面的链表称为old列表区，存放使用较少数据，占37%。  

![1636995450(1)](D:\study\github\study_lagou\笔记\第四阶段\mysqlMd\1636995450(1).jpg)

https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool.html

- Change Buffer：写缓冲区,在进行DML操作时，如果BP没有其相应的Page数据，并不会立刻将磁盘页加载到缓冲池，而是在CB记录缓冲变更，等未来数据被读取时，再将数据合并恢复到BP中

- Adaptive Hash Index：自适应哈希索引，用于优化对BP数据的查询。InnoDB存储引擎会监控对表索引的查找，如果观察到建立哈希索引可以带来速度的提升，则建立哈希索引，所以称之为自适应。InnoDB存储引擎会自动根据访问的频率和模式来为某些页建立哈希索引。  

- Log Buffer：日志缓冲区，用来保存要写入磁盘上log文件（Redo/Undo）的数据，日志缓冲区的内容定期刷新到磁盘log文件中。  

##### 磁盘结构:

- 双写缓冲区（Doublewrite Buffer）:双写缓冲区是一个存储区域，在 `InnoDB`将页面写入`InnoDB`数据文件中的适当位置之前，从缓冲池中写入页面 。如果 在页面写入过程中存在操作系统、存储子系统或意外的[**mysqld**](https://dev.mysql.com/doc/refman/8.0/en/mysqld.html)进程退出，则 `InnoDB`可以在崩溃恢复期间从双写缓冲区中找到该页面的良好副本。

- 重做日志（Redo Log）  :重做日志是一种基于磁盘的数据结构，用于在崩溃恢复期间更正不完整事务写入的数据。MySQL以循环方式写入重做日志文件，记录InnoDB中所有对Buffer Pool修改的日志。  