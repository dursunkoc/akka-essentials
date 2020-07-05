package playground

object ThreadModelLimitations extends App {

  /**
   * OOP encapsulation is only valid on the SINGLE THREAD MODEL.
   */

  class BankAccount(private var balance: Int) {
    def getBalance = this.balance

    override def toString: String = "" + balance

    def withdraw(amount: Int) = this.balance -= amount
    def deposit(amount: Int) = this.balance += amount
  }

  val account = new BankAccount(2000)
  for (_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }
  for (_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }

  println(account.getBalance)
  Thread.sleep(3000)
  println(account.getBalance)
}
