# redis_fbs_sock
redis分布式锁
//业务代码操作直接报错了  导致锁一直没释放  后面一直加不上锁
所以将释放锁添加到finally里面
本地修改代码后需要Git push到github
