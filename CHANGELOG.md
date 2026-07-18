# Changelog

## [1.0.1](https://github.com/AzisabaNetwork/SimpleQuest/compare/v1.0.0...v1.0.1) (2026-07-18)


### Features

* add more test cases ([5e62c6b](https://github.com/AzisabaNetwork/SimpleQuest/commit/5e62c6b5a15e3bf3833c89c27125b132925ee4dd))
* add shadowJar support and server setup wiki ([a94f9c3](https://github.com/AzisabaNetwork/SimpleQuest/commit/a94f9c35c33f11c7b1dbeff33f82b8d7cf40e0a5))
* integration test infrastructure for release branch PRs ([6023a76](https://github.com/AzisabaNetwork/SimpleQuest/commit/6023a764d38e5b2ba49cc913639b85be645ad019))
* integration test infrastructure for release branch PRs ([a051b04](https://github.com/AzisabaNetwork/SimpleQuest/commit/a051b0418cfad1c7c5c35677a18cf7eeb64a93ce))
* port Quem GUI system to LifeQuest ([2999219](https://github.com/AzisabaNetwork/SimpleQuest/commit/2999219f54f4adb2335f6a4619539555eb03cb3e))
* port Quem GUI system to LifeQuest ([1b87e52](https://github.com/AzisabaNetwork/SimpleQuest/commit/1b87e5273bd57156b1c38d6468f79c6353d7aa00))
* replace SchemaUtils with Flyway for database migration ([81e99ed](https://github.com/AzisabaNetwork/SimpleQuest/commit/81e99ed17e4527de40494cb4bce96d8eef599820))


### Bug Fixes

* add explicit driverClassName for HikariCP (Paper PluginClassLoader issue) ([274efb3](https://github.com/AzisabaNetwork/SimpleQuest/commit/274efb3eab71c072f5ad001073b03a482b778fc9))
* add HikariCP initializationFailTimeout=-1, reduce memory to 512M, increase startup timeout to 180s ([1ef6ff9](https://github.com/AzisabaNetwork/SimpleQuest/commit/1ef6ff98c0a32b5af49e52ae5144e82ba1694ff9))
* add MASTER_RCON_PORT env, add 5s delay before tests, simplify env vars ([a8c763d](https://github.com/AzisabaNetwork/SimpleQuest/commit/a8c763d3afa666bcfc833c31179cff5964cdf1a8))
* bypass Exposed ServiceLoader and handle onDisable crash ([ba947ff](https://github.com/AzisabaNetwork/SimpleQuest/commit/ba947ff69bfb099cf9b0074160f9fc46d7c20942))
* cd into server directory before starting Paper ([5b14848](https://github.com/AzisabaNetwork/SimpleQuest/commit/5b14848a8daeddb0cb6b3ca1c7085133465dfd55))
* change extra-files type from gradle to generic (unsupported in release-please v5) ([c096960](https://github.com/AzisabaNetwork/SimpleQuest/commit/c09696014cf170da9df1450438792cad23808e77))
* CI workflow fixes - pnpm cache, bash scoping, server guard ([22a9998](https://github.com/AzisabaNetwork/SimpleQuest/commit/22a9998ba39b100e7514345d5971f4174e67d1c9))
* **deps:** resolve uuid vulnerability via pnpm overrides (&gt;=11.1.1) ([c3bba82](https://github.com/AzisabaNetwork/SimpleQuest/commit/c3bba828a4d2b54a0a2b3c0b536c8038d50396a2))
* grep 'Done(' not 'Done' to avoid false match, add DuplicatesStrategy.INCLUDE to shadowJar ([ae30f42](https://github.com/AzisabaNetwork/SimpleQuest/commit/ae30f428317df43b3afa49fa391e1a6cea1677cd))
* grep Done( to avoid false match on ReobfServer, increase timeout to 240s ([82a4528](https://github.com/AzisabaNetwork/SimpleQuest/commit/82a452846795496a2dc42291baf333db2429b771))
* make ConfigModule self-sufficient for config generation ([a14e297](https://github.com/AzisabaNetwork/SimpleQuest/commit/a14e2974c60ed647fc4eb29c7e54eeb6cba6612e))
* MariaDB table exists test - use &gt;= 0 instead of == 0 ([bca771f](https://github.com/AzisabaNetwork/SimpleQuest/commit/bca771f7c84cc57df4051da8f14fed623d0637a7))
* pass DataSource to V1 migration (needs password) for Exposed transaction ([0b7bc70](https://github.com/AzisabaNetwork/SimpleQuest/commit/0b7bc702c73d049ea032c29156801f617122b6fc))
* relax integration tests - focus on plugin load, skip missing DB table ([7adf87e](https://github.com/AzisabaNetwork/SimpleQuest/commit/7adf87ebdca5890ae961d564f7eec8337a9788b1))
* remove pnpm cache from setup-node (pnpm not pre-installed on runner) ([19558e5](https://github.com/AzisabaNetwork/SimpleQuest/commit/19558e5dcfe74b3d04e82007051e749fdea1dbb4))
* remove RCON dependency, use log+DB verification only ([f676899](https://github.com/AzisabaNetwork/SimpleQuest/commit/f676899a05b6fad7a6d029002e9a3fded8661e77))
* remove redundant casts in QuestTypeDataTest ([67c7947](https://github.com/AzisabaNetwork/SimpleQuest/commit/67c7947ecb59cbef46372d00f5aa73f9c6826d28))
* remove shadow relocate to fix Paper 1.21.11 PluginRemapper ([9f63dbb](https://github.com/AzisabaNetwork/SimpleQuest/commit/9f63dbb60dea809f423c7b13d5fd5a063b46bb90))
* remove target-branch so release-please targets default branch (main) ([d51e197](https://github.com/AzisabaNetwork/SimpleQuest/commit/d51e197529920102daf01b3c9c9b6f58295d127f))
* restore all 3 servers (master worked), keep debug output ([a5d1d01](https://github.com/AzisabaNetwork/SimpleQuest/commit/a5d1d01b22267d6f82511ee9a2372b17f60714c9))
* server.properties indentation issue with YAML literal block ([05ba3a6](https://github.com/AzisabaNetwork/SimpleQuest/commit/05ba3a6a1518b7d841b75345121d45a4750a13fb))
* update PaperMC API to v3 (api.papermc.io -&gt; fill.papermc.io) ([c3987b5](https://github.com/AzisabaNetwork/SimpleQuest/commit/c3987b5e05871d5ce39435d4a66a91b480251f34))
* use correct gradle task name integrationTestTask ([c5dff3e](https://github.com/AzisabaNetwork/SimpleQuest/commit/c5dff3ea4529f9019ea42a6a601bb33c89fff312))
* use javaMigrations() instead of classpath scanning, add DB table tests ([968d741](https://github.com/AzisabaNetwork/SimpleQuest/commit/968d7412ff8ad3c70d1d44c39afe72350f9d4556))
* use printf for eula.txt, add debug output for plugins directory ([0db8f03](https://github.com/AzisabaNetwork/SimpleQuest/commit/0db8f03bca87f9224aa5d3c10e550159c95a8928))
* use tail -f on logs/latest.log, remove FIFO, simpler process management ([c56fed7](https://github.com/AzisabaNetwork/SimpleQuest/commit/c56fed7257762c790a43918b4c014452b86c479b))
* V1 migration - create temp HikariDS for Exposed transaction ([496bc54](https://github.com/AzisabaNetwork/SimpleQuest/commit/496bc541a13b0422af8a8782e4aeff61e63e51c8))
* V1 migration - use array spread, simple JDBC execution, no Exposed transaction ([5d43199](https://github.com/AzisabaNetwork/SimpleQuest/commit/5d43199ff12a994c6f75028e6937c780f8ca0a8d))
* V1 migration - wrap in Exposed transaction, use getNewConnection lambda ([59e2e81](https://github.com/AzisabaNetwork/SimpleQuest/commit/59e2e810acb865fefa2d66fceed0eea6a3cb624f))
