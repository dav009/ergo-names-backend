package ergonames.GenerateMintContractAddress

import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}

object GenerateMintContractAddress {
  def generateMintContractAddress(configFileName: String): String = {
    // Node configuration values
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String = RestApiErgoClient.getDefaultExplorerUrl(NetworkType.TESTNET)

    // Fetch parameters from config
    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt

    // Create ErgoClient instance (represents a connection to node)
    var ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)

    // Execute transaction
    val txJson: String = ergoClient.execute((ctx: BlockchainContext) => {

      // Initialize prover (signs the transaction)
      val senderProver: ErgoProver = ctx.newProverBuilder
        .withMnemonic(
          SecretString.create(nodeConf.getWallet.getMnemonic),
          SecretString.create(nodeConf.getWallet.getPassword)
        )
        .withEip3Secret(addressIndex)
        .build()

      // Get input (spending) boxes from node wallet
      val wallet: ErgoWallet = ctx.getWallet
      val amountToSend: Long = Parameters.MinChangeValue
      val totalToSpend: Long = amountToSend + Parameters.MinFee
      val boxes: java.util.Optional[java.util.List[InputBox]] = wallet.getUnspentBoxes(totalToSpend)
      if (!boxes.isPresent)
        throw new ErgoClientException(s"Not enough coins in the wallet to pay $totalToSpend", null)


     val (tx, contract) = assembleTransaction(ctx, amountToSend, boxes.get, senderProver.getAddress())

      // Sign transaction with prover
      val signed: SignedTransaction = senderProver.sign(tx)

      // Submit transaction to node
      val txId: String = ctx.sendTransaction(signed)

      // Return transaction as JSON string
      signed.toJson(true)
    })
    txJson
  }

  def assembleTransaction(ctx: BlockchainContext, amountToSend: Long,  boxes: java.util.List[InputBox], senderAddress:Address )= {
      // Define protection script
      // The script expects an NFT to be issued, and that it be issued by a specific wallet
      val mintingContract: String = s"""
      {
        val proposedTokenHasSameIdAsFirstTxInput = OUTPUTS(0).tokens(0)._1 == SELF.id
        val proposedTokenIsNonFungible = OUTPUTS(0).tokens(0)._2 == 1
        val proposedTokenIsValidNFT = proposedTokenHasSameIdAsFirstTxInput && proposedTokenIsNonFungible

        sigmaProp(proposedTokenIsValidNFT && ergoNamesPk)
      }
      """.stripMargin

      // Create unsigned tx builder
      val transactionBuilder: UnsignedTransactionBuilder = ctx.newTxBuilder

    // Create output box
    val ergoNamesPk: Address = Address.create(senderAddress.asP2PK().toString)
    val contract = ctx.compileContract(
            ConstantsBuilder.create()
              .item("ergoNamesPk", ergoNamesPk.getPublicKey)
              .build(),
            mintingContract)
     
      val newBox: OutBox = transactionBuilder.outBoxBuilder
        .value(amountToSend)
        .contract(contract)
        .build()

      // Create unsigned transaction
      val tx: UnsignedTransaction = transactionBuilder
        .boxesToSpend(boxes)
        .outputs(newBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(senderAddress.asP2PK())
        .build()
    (tx, contract)
  }

  def main(args: Array[String]) : Unit = {
    val txJson = generateMintContractAddress("config.json")
    print(txJson)
  }
}
