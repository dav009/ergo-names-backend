package ergonames.NodeConfiguration

import org.ergoplatform.appkit.ErgoClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.appkit.config.{ErgoToolConfig, ErgoNodeConfig}

object NodeTools {

    private val defaultMainnetExplorerUrl = "https://api.ergoplatform.com"
    private val defaultTestnetExplorerUrl = "https://api-testnet.ergoplatform.com"

    def createToolConfig(configFile: String): ErgoToolConfig = {
        val config: ErgoToolConfig = ErgoToolConfig.load(configFile)
        return config
    }

    def createNodeConfig(toolConfig: ErgoToolConfig): ErgoNodeConfig = {
        val config: ErgoNodeConfig = toolConfig.getNode()
        return config 
    }

    def createErgoClient(nodeConfig: ErgoNodeConfig): ErgoClient = {
        val client: ErgoClient = RestApiErgoClient.create(nodeConfig, defaultTestnetExplorerUrl)
        return client
    }
}
