package ergonames.Minter

import ergonames.NodeConfiguration.NodeWallet.getWalletMnemonic
import ergonames.NodeConfiguration.NodeWallet.getPublicErgoAddress

import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.ErgoNodeConfig
import org.ergoplatform.ErgoAddress

object MintToken {

  def mint(nodeConfig: ErgoNodeConfig, client: ErgoClient, tokenName: String, tokenDescription: String, recieverWalletAddress: Address): String = {
    val transactionJson: String = client.execute((ctx: BlockchainContext) => {
      val amountToSpend: Long = Parameters.MinChangeValue
      val totalToSpend: Long = amountToSpend + Parameters.MinFee

      val mnemonic: Mnemonic = getWalletMnemonic(nodeConfig)

      val senderProver: ErgoProver = ctx.newProverBuilder()
        .withMnemonic(
          SecretString.create(nodeConfig.getWallet().getMnemonic()),
          SecretString.create(nodeConfig.getWallet().getPassword()))
        .withEip3Secret(0)
        .build()
      
      val senderAddress: Address = getPublicErgoAddress(nodeConfig)
      // val senderAddress1: Address = senderProver.getAddress()

      val unspentBoxes: java.util.List[InputBox] = ctx.getUnspentBoxesFor(senderAddress, 0, 20)
      val boxesToSpend: java.util.List[InputBox] = BoxOperations.selectTop(unspentBoxes, totalToSpend)

       val transactionBuilder: UnsignedTransactionBuilder = ctx.newTxBuilder()

      val transaction = ensembleTransaction(ctx, transactionBuilder, amountToSpend, boxesToSpend, recieverWalletAddress, tokenName, tokenDescription)

      val signedTransaction: SignedTransaction = senderProver.sign(transaction)

      val transactionId: String = ctx.sendTransaction(signedTransaction)

      signedTransaction.toJson(true)
    })
    transactionJson
  }

   def createOutBox(ctx: BlockchainContext, transactionBuilder: UnsignedTransactionBuilder, amountToSpend: Long, token: ErgoToken, tokenName: String, tokenDescription: String, recieverWalletAddress: Address): OutBox = {
    val outBox: OutBox = transactionBuilder.outBoxBuilder()
      .value(amountToSpend)
      .mintToken(token, tokenName, tokenDescription, 0)
      .contract(ctx.compileContract(
        ConstantsBuilder.create()
          .item("recieverPublicKey", recieverWalletAddress.getPublicKey())
          .build(),
          "{ recieverPublicKey }")
      )
      .build()

      return outBox
   }

  def ensembleTransaction(ctx: BlockchainContext, transactionBuilder: UnsignedTransactionBuilder, amountToSpend: Long,  boxesToSpend: java.util.List[InputBox], recieverWalletAddress: Address, tokenName: String, tokenDescription: String) = {
      val domainToken: ErgoToken = new ErgoToken(boxesToSpend.get(0).getId(), 1)

     

      val outBox: OutBox = createOutBox(ctx, transactionBuilder, amountToSpend, domainToken, tokenName, tokenDescription, recieverWalletAddress)

    createTransaction(transactionBuilder, boxesToSpend, outBox, recieverWalletAddress)
  }

  def createTransaction(transactionBuilder: UnsignedTransactionBuilder, boxesToSpend: java.util.List[InputBox], outBox: OutBox, recieverWalletAddress: Address): UnsignedTransaction = {
    val transaction: UnsignedTransaction = transactionBuilder
      .boxesToSpend(boxesToSpend)
      .outputs(outBox)
      .fee(Parameters.MinFee)
      .sendChangeTo(recieverWalletAddress.asP2PK())
      .build()

      return transaction
  }


}
