package ergonames.SpendBoxAtMintContractAddress

import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.ErgoTreeContract

import java.util.stream.Collectors

object SpendBoxAtMintContractAddress {
  def spendBoxAtContractAddress(configFileName: String): String = {
    // Node configuration values
    val conf: ErgoToolConfig = ErgoToolConfig.load(configFileName)
    val nodeConf: ErgoNodeConfig = conf.getNode
    val explorerUrl: String = RestApiErgoClient.getDefaultExplorerUrl(NetworkType.TESTNET)

    // Fetch parameters from config
    val nftReceiverAddress: Address = Address.create(conf.getParameters.get("nftReceiverAddress"))
    val addressIndex: Int = conf.getParameters.get("addressIndex").toInt
    val mintingContractAddress: String = conf.getParameters.get("mintingContractAddress")
    val nftMintRequestBoxId: String = conf.getParameters.get("nftMintRequestBoxId")

    // Create ErgoClient instance (represents a connection to node)
    val ergoClient: ErgoClient = RestApiErgoClient.create(nodeConf, explorerUrl)

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

      // Get input (spending) boxes from minting contract address
      val spendingAddress: Address = Address.create(mintingContractAddress)


         // Amount to retrieve from nft minting request box - this is essentially collecting payment for the mint
     val amountToSend: Long = 25000000L - Parameters.MinFee
      
      val tx = assembleTransaction(ctx, spendingAddress, ErgoId.create(nftMintRequestBoxId), nftReceiverAddress , senderProver.getAddress(),amountToSend )

      // Sign transaction with prover
      val signed: SignedTransaction = senderProver.sign(tx)

      // Submit transaction to node
      val txId: String = ctx.sendTransaction(signed)

      // Return transaction as JSON string
      signed.toJson(true)
    })
    txJson
  }

    def assembleTransaction(ctx: BlockchainContext, spendingAddress: Address, nftMintRequestBoxId: ErgoId, nftReceiverAddress: Address, senderAddress: Address, amountToSend: Long)= {
      val spendingBoxes: java.util.List[InputBox] = ctx.getUnspentBoxesFor(spendingAddress, 0, 20)
        .stream()
        .filter(_.getId == nftMintRequestBoxId)
        .collect(Collectors.toList())

   

      // Create unsigned tx builder
      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder

      // Create output box
      val proposedToken: ErgoToken = new ErgoToken(spendingBoxes.get(0).getId, 1)
      val tokenName: String = "contract_issued_nft_test.erg"
      val tokenDescription: String = "Early stage testing of token minting with contracts"
      val tokenDecimals: Int = 0

      val newBox: OutBox = txB.outBoxBuilder
        .mintToken(proposedToken, tokenName, tokenDescription, tokenDecimals)
        .value(amountToSend)
        .contract(new ErgoTreeContract(nftReceiverAddress.getErgoAddress.script))
        .build()

      // Create unsigned transaction
      val tx: UnsignedTransaction = txB
        .boxesToSpend(spendingBoxes)
        .outputs(newBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(senderAddress.asP2PK())
        .build()

      tx
  }

  def main(args: Array[String]): Unit = {
    val txJson = spendBoxAtContractAddress("config.json")
    print(txJson)
  }
}
