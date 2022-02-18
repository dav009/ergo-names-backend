package ergonames.Minter
import ergonames.GenerateMintContractAddress.GenerateMintContractAddress
import ergonames.SpendBoxAtMintContractAddress.SpendBoxAtMintContractAddress
import com.dav009.ergopilot.Simulator._
import com.dav009.ergopilot.model.{TokenAmount, TokenInfo}
import org.ergoplatform.appkit._
import org.ergoplatform.P2PKAddress
import org.scalatest.{PropSpec, Matchers}
import org.ergoplatform.ErgoAddressEncoder
import org.scalatest._
import org.scalatest.{Matchers, WordSpecLike}

class WorkflowSpec extends WordSpecLike with Matchers {

  

  "test workflow" in {

    val (blockchainSim, ergoClient) =
      newBlockChainSimulationScenario("Workflow test")

    ergoClient.execute((ctx: BlockchainContext) => {

      val ergoNames = blockchainSim.newParty("ergoNames", ctx)
      val customer = blockchainSim.newParty("customer", ctx)
      val initialAmount = 10000000000L
      ergoNames.generateUnspentBoxes(toSpend = initialAmount)
       customer.generateUnspentBoxes(toSpend = initialAmount)
      
      
      
      val amountToSpend =  Parameters.MinChangeValue +  Parameters.MinFee
      val boxes = ergoNames.wallet.getUnspentBoxes(amountToSpend).get

      // create generate mint contract adddress with ergonames wallet
      val (tx, contract) = GenerateMintContractAddress.assembleTransaction(ctx,amountToSpend, boxes, ergoNames.wallet.getAddress)

  
      ctx.sendTransaction(ergoNames.wallet.sign(tx))
      

      val spendingAdd: Address =  Address.fromErgoTree(contract.getErgoTree(), NetworkType.MAINNET)

      // spend Box at Mint ccontract address
      val nftReceiverAddress = customer.wallet.getAddress
      val nftMintRequestBoxId = ctx.getUnspentBoxesFor(spendingAdd, 0, Int.MaxValue).get(0).getId()
      val spendingAddress = spendingAdd
      val senderAddress = ergoNames.wallet.getAddress
      val tx2 = SpendBoxAtMintContractAddress.assembleTransaction(ctx,
        spendingAddress, nftMintRequestBoxId, nftReceiverAddress, senderAddress, Parameters.MinChangeValue)

      ctx.sendTransaction(ergoNames.wallet.sign(tx2))

      val minterOwnedTokens =
        blockchainSim.getUnspentTokensFor(customer.wallet.getAddress)
      assert(
        minterOwnedTokens == List(
          new TokenAmount(new TokenInfo(nftMintRequestBoxId, "contract_issued_nft_test.erg"), 1)
        )
      )

    })
  }
}
